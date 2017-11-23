#!/bin/sh

set -e -x

if [ "$#" -ne 1 ]; then
    echo "Please provide location to place build output."
    exit 1;
fi

./mvnw package

zip -r $1/route-service.zip *
