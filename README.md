[![Build Status](https://travis-ci.org/flowcommerce/location.svg?branch=master)](https://travis-ci.org/flowcommerce/location)

location
=========
Retrieves Geolocation information base on IP address

# Geolocation

Main geolocation data is pulled from (GeoLite2-City.mmdb)

    http://dev.maxmind.com/geoip/geoip2/geolite2/

Ip address -> location data is from DigitalElement's Edge database:  https://portal.digitalelement.com/portal/availableDatabases/index.html
(creds in LastPass)

Their data is packaged in a proprietary, binary multifile format.  They do, however, provide a "Text File Creator" [tool](https://portal.digitalelement.com/portal/tools/index.html) to export it to CSV format.

Until this process is automated, an export can be created like so:
```bash
netacuity-textfile-creator.sh --db_path=<path_to_extracted_edge_database> --db=4 --numeric --fields=edge-country,edge-region,edge-city,edge-latitude,edge-longitude,edge-postal-code --output_file=./<version_number>.csv
```

The resulting file should then be uploaded to `s3://io-flow-location/digitalelement/edge/` and the `digitalelement.file.uri` property of the config should be updated accordingly

The default value of the property points to a truncated version of the file at `s3://io-flow-location/digitalelement/edge/0508_mini.csv`.  If you want to use a local file you can override the property with the `DIGITALELEMENT_FILE_URI` environment variable using a `file://` uri.

Tests are run against a sample dataset that is committed to VC in `./test/resources/digitalelement_sample.csv`. If you need to test against specific data, be sure to update that file.  (it can also be used as the `DIGITALELEMENT_FILE_URI` if you need to run the app locally without access to the S3 bucket).

If you are running out of memory or having GC issues when trying to build the full index, you probably need to increase SBT's heap size by setting the SBT_OPTS environment var: `SBT_OPTS="-Xms2G"`

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

