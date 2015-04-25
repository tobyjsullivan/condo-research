#!/bin/sh
CURRENT_PATH=`pwd`
SECRETS="$CURRENT_PATH/secrets.conf"
sbt assembly
echo Run with secrets: $SECRETS
java -Dconfig.file=$SECRETS -jar ./target/scala-2.11/condo-research-assembly-1.0.jar
