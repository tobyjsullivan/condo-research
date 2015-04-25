package com.krumptium.condo_research

import java.io.{FileReader, FileWriter, File}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.typesafe.config.ConfigFactory
import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

import scala.collection.JavaConversions._
import scala.io.Source

object CondoResearch extends App {
  val config = ConfigFactory.load()

  val awsKey = config.getString("aws.iam.accessKeyId")
  val awsSecret = config.getString("aws.iam.secretAccessKey")
  val bucketName = config.getString("aws.s3.bucket")
  val keyPrefix = config.getString("aws.s3.keyPrefix")

  val baseUrl: String = config.getString("crawler.listingsUrl")

  val humanizedClickDelay: Int = 3 * 1000 // 3 Seconds
  val pauseBetweenSearches: Int = 49 * 60 * 1000 // 49 minutes

  val researchHistoryFilePath = "research-history.csv"
  val historyFile = new File(researchHistoryFilePath)

  var researchedListings: Set[String] = loadResearchHistory()

  // Print configs to help ensure loaded
  println("Key: "+awsKey)
  println("S3 Bucket: "+bucketName)
  println("keyPrefix: "+keyPrefix)
  println("Base URL: "+baseUrl)

  val s3client = new AmazonS3Client(new BasicAWSCredentials(awsKey, awsSecret))

  while (true) {
    crawlRecentResults()

    println("Finished current search. Waiting a few minutes.")

    Thread.sleep(pauseBetweenSearches) // Pause to not spam the server with searches
  }
  
  def loadResearchHistory(): Set[String] = {
    if (historyFile.exists()) {
      val reader = Source.fromFile(historyFile)
      val history = reader.getLines().toSet
      reader.close()
      history
    } else
      Set()
  }

  def addListingToHistory(listingNumber: String): Unit = {
    researchedListings += listingNumber
    saveResearchHistory(researchedListings)
  }

  def saveResearchHistory(history: Set[String]): Unit = {
    val output = history.mkString("\n")

    if (output.length > 0) {
      val writer = new FileWriter(historyFile)
      writer.write(output)
      writer.close()
    }
  }

  def uploadFileToS3(file: File): Unit = {
    val keyName = keyPrefix + file.getName

    println("Uploading to: "+keyName)
    s3client.putObject(new PutObjectRequest(bucketName, keyName, file))
  }

  def crawlRecentResults(): Unit = {
    val propertyDetailGridSelector = "//table[contains(@class,'PropertyDetailGridTableBorder')]"
    def listingNumberSelector(gridId: String): String = s"//table[@id='$gridId']/tbody/tr/td[2]/table/tbody/tr/td[1]/b"
    def viewDetailsSelector(gridId: String): String = s"//table[@id='$gridId']//div[@id='divViewDetails']"

    def getPropertyListings(driver: WebDriver): List[WebElement] =
      driver.findElements(By.xpath(propertyDetailGridSelector)).toList

    def getPropertyListing(driver: WebDriver, idx: Int): WebElement = {
      val listings = getPropertyListings(driver) // We re-fetch all the listings because they are invalid once page changes
      listings(idx)
    }

    def getListingTableId(propertyListing: WebElement): String =
      propertyListing.getAttribute("id")

    def getListingNumber(driver: WebDriver, listingTableId: String) =
      driver.findElement(By.xpath(listingNumberSelector(listingTableId))).getText

    def getViewDetailsButton(driver: WebDriver, listingTableId: String): WebElement =
      driver.findElement(By.xpath(viewDetailsSelector(listingTableId)))

    val driver: WebDriver = new FirefoxDriver()

    driver.get(baseUrl)

    Thread.sleep(humanizedClickDelay) // Simulate real human

    val actualTitle: String = driver.getTitle

    println("Title: " + actualTitle)

    val listings = getPropertyListings(driver)
    val numListings = listings.size

    val listingNumberToTableId: Map[String, String] =
      listings.map { propertyListingElement =>
        val tableId = getListingTableId(propertyListingElement)
        val listingNumber = getListingNumber(driver, tableId)
        listingNumber -> tableId
      }.toMap

    for (
      (listingNumber, tableId) <- listingNumberToTableId
    ) yield {
      println(s"Found listing for: $listingNumber (elementID: $tableId)")

      if (!researchedListings.contains(listingNumber)) {
        println("Researching new listing...")

        val viewDetailsButton = getViewDetailsButton(driver, tableId)

        viewDetailsButton.click()

        Thread.sleep(humanizedClickDelay) // Simulate real human

        val addressElement = driver.findElement(By.id("spanAddressClassifier"))
        val address = addressElement.getText

        println("Address: " + address)

        val listingPageSource = driver.getPageSource
        val oListingNumber = "V[0-9]{7}".r findFirstIn listingPageSource

        println("Listing #: " + oListingNumber.getOrElse(""))

        val iframe = driver.findElement(By.xpath("//iframe[@id='reportFrame']"))

        val iframeSrc = iframe.getAttribute("src")
        println("Report URL: " + iframeSrc)

        val frameDriver = driver.switchTo().frame(iframe)


        val reportSourceCode = frameDriver.getPageSource
        val srcOutput = File.createTempFile("condo-research_" + oListingNumber.getOrElse("") + "_", ".html")
        val writer = new FileWriter(srcOutput)
        writer.write(reportSourceCode)
        writer.close()

        println("Source written to: " + srcOutput.getAbsolutePath)

        uploadFileToS3(srcOutput)

        srcOutput.delete()
        println("File deleted")

        val parentDriver = frameDriver.switchTo().parentFrame()

        parentDriver.navigate().back()

        Thread.sleep(2000) // We legitimately have to wait for back-nav to load

        addListingToHistory(listingNumber)
      }


    }

    driver.close()
  }
}
