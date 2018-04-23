terraform {
  backend "s3" {
    bucket = "terraform-states.tobyjsullivan.com"
    key    = "states/condo-research/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = "us-west-2"
}

resource "aws_s3_bucket" "data" {
  acl = "private"
}

resource "aws_s3_bucket_policy" "data" {
  bucket = "${aws_s3_bucket.data.id}"
  policy =<<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AmazonML_s3:ListBucket",
            "Effect": "Allow",
            "Principal": {
                "Service": "machinelearning.amazonaws.com"
            },
            "Action": "s3:ListBucket",
            "Resource": "arn:aws:s3:::${aws_s3_bucket.data.id}",
            "Condition": {
                "StringLike": {
                    "s3:prefix": [
                        "sold-data/MLS_DATA_2015-05-27-003.csv*",
                        "sold-data/Extended Sold Data - Enhanced - Stripped.csv*",
                        "sold-data/Listings - Enhanced - Unpredicted Data.csv*",
                        "sold-data/Extended Sold Data - Enhanced.csv*",
                        "model-tests/*",
                        "sold-data/Listings - Enhanced - listings__2015_04_21_14_31_30.csv*",
                        "sold-data/Listings - Enhanced - Copy of ML Datasource.csv*",
                        "sold-data/Extended Sold Data - Enhanced - Stripped (2).csv*",
                        "model-tests/Extended Sold Data - Enhanced - Filtered and Stripped.csv*",
                        "sold-data/MLS_DATA_2015-05-27-002.csv*",
                        "sold-data/Extended Sold Data - Enhanced - Stripped (1).csv*",
                        "predictions/output-2015-04-25-001*",
                        "sold-data/MLS_DATA_2015-05-27.csv*",
                        "model-tests/Extended Sold Data - Enhanced - Filtered and Stripped (1).csv*",
                        "sold-data/Extended Sold Data - Enhanced - Sheet1.csv*"
                    ]
                }
            }
        },
        {
            "Sid": "AmazonML_s3:GetObject",
            "Effect": "Allow",
            "Principal": {
                "Service": "machinelearning.amazonaws.com"
            },
            "Action": "s3:GetObject",
            "Resource": [
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Extended Sold Data - Enhanced - Stripped.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/model-tests/Extended Sold Data - Enhanced - Filtered and Stripped (1).csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Listings - Enhanced - listings__2015_04_21_14_31_30.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Listings - Enhanced - Copy of ML Datasource.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/MLS_DATA_2015-05-27.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/model-tests/*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Extended Sold Data - Enhanced.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Extended Sold Data - Enhanced - Sheet1.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Extended Sold Data - Enhanced - Stripped (2).csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/predictions/output-2015-04-25-001*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/model-tests/Extended Sold Data - Enhanced - Filtered and Stripped.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Extended Sold Data - Enhanced - Stripped (1).csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/MLS_DATA_2015-05-27-002.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/Listings - Enhanced - Unpredicted Data.csv*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/sold-data/MLS_DATA_2015-05-27-003.csv*"
            ]
        },
        {
            "Sid": "AmazonML_s3:PutObject",
            "Effect": "Allow",
            "Principal": {
                "Service": "machinelearning.amazonaws.com"
            },
            "Action": "s3:PutObject",
            "Resource": [
                "arn:aws:s3:::${aws_s3_bucket.data.id}/model-tests/*",
                "arn:aws:s3:::${aws_s3_bucket.data.id}/predictions/output-2015-04-25-001*"
            ]
        }
    ]
}
POLICY
}
