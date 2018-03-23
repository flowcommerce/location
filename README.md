[![Build Status](https://travis-ci.org/flowcommerce/location.svg?branch=master)](https://travis-ci.org/flowcommerce/location)

location
=========
Retrieves Geolocation information base on IP address

# Geolocation Database

See https://www.notion.so/flow/Updating-DigitalElement-Edge-Database-ce2c3836ec9e4121bb14fdb0f3d4cd53

# Usage

Sample Request:
    http://localhost:9000/addresses?ip=23.16.0.0

Sample Response:
```
    {
      "address": {
        "city": "Sparwood",
        "country": "CAN"
      },
      "latitude": "49.7333",
      "longitude": "-114.8853"
    }
```

