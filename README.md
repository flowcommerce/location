[![Build Status](https://travis-ci.org/flowcommerce/location.svg?branch=master)](https://travis-ci.org/flowcommerce/location)


location
=========
Retrieves Geolocation information base on IP address

# Geolocation

Main geolocation data is pulled from (GeoLite2-City.mmdb)

    http://dev.maxmind.com/geoip/geoip2/geolite2/

# Dependencies

Service uses the following client

    https://github.com/Sanoma-CDA/maxmind-geoip2-scala

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

