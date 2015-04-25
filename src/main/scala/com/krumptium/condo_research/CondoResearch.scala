package com.krumptium.condo_research

import java.io.{FileWriter, File}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.typesafe.config.ConfigFactory
import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

import scala.collection.JavaConversions._

object CondoResearch extends App {
  val config = ConfigFactory.load()

  val awsKey = config.getString("aws.iam.accessKeyId")
  val awsSecret = config.getString("aws.iam.secretAccessKey")
  val bucketName = config.getString("aws.s3.bucket")
  val keyPrefix = config.getString("aws.s3.keyPrefix")

  val baseUrl: String = config.getString("crawler.listingsUrl")

  val humanizedClickDelay: Int = 3 * 1000 // 3 Seconds
  val pauseBetweenSearches: Int = 49 * 60 * 1000 // 49 minutes

  val viewDetailsSelector = "//div[@id='divViewDetails']"

  // Print configs to help ensure loaded
  println("Key: "+awsKey)
  println("S3 Bucket: "+bucketName)
  println("keyPrefix: "+keyPrefix)
  println("Base URL: "+baseUrl)

  val s3client = new AmazonS3Client(new BasicAWSCredentials(awsKey, awsSecret))

  while (true) {
    crawlRecentResults()

    Thread.sleep(pauseBetweenSearches) // Pause to not spam the server with searches
  }

  def uploadFileToS3(file: File): Unit = {
    val keyName = keyPrefix + file.getName

    println("Uploading to: "+keyName)
    s3client.putObject(new PutObjectRequest(bucketName, keyName, file))
  }

  def crawlRecentResults(): Unit = {
    val driver: WebDriver = new FirefoxDriver()

    driver.get(baseUrl)

    Thread.sleep(humanizedClickDelay) // Simulate real human

    val actualTitle: String = driver.getTitle

    println("Title: " + actualTitle)

    val numDetailsElements: Int = driver.findElements(By.xpath(viewDetailsSelector)).size()

    def getDetailsElement(driver: WebDriver, idx: Int): WebElement =
      driver.findElements(By.xpath(viewDetailsSelector)).toList(idx)


    for (
      i <- 0 until numDetailsElements
    ) yield {
      val element = getDetailsElement(driver, i)
      println("Details Tag: " + element.getTagName)

      element.click()

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
    }

    driver.close()
  }
}
