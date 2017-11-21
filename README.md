[![Build Status](https://travis-ci.org/flowcommerce/location.svg?branch=master)](https://travis-ci.org/flowcommerce/location)

location
=========
Retrieves Geolocation information base on IP address

# Geolocation

Ip address -> location data is from DigitalElement's Edge database:  https://portal.digitalelement.com/portal/availableDatabases/index.html
(creds in LastPass)

Their data is packaged in a proprietary, binary multifile format.  To create a usable text version, do the following:

1. Download their "Text File Creator" [tool](https://portal.digitalelement.com/portal/tools/index.html), used to convert the binary database to CSV format.
2. Download the latest version of the database https://portal.digitalelement.com/portal/availableDatabases/index.html
3. Extract the database into its own directory (it contains multiple files)
4. Create a text file version of the db:
```bash
$ netacuity-textfile-creator.sh --db_path=<path_to_extracted_edge_database> --db=4 --numeric --fields=edge-country,edge-region,edge-city,edge-latitude,edge-longitude,edge-postal-code --output_file=./<version_number>.csv
```
5. Create a text file version of the db with ipv6 addresses:
```bash
$ netacuity-textfile-creator.sh --db_path=<path_to_extracted_edge_database> --db=4 --ipv6 --numeric --fields=edge-country,edge-region,edge-city,edge-latitude,edge-longitude,edge-postal-code --output_file=./<version_number>_ipv6.csv
```
6. DigitalElement separates the network and interface groups of the address, and thus, has two extra fields that we must remove before we append them to the original file:
```bash
$ cut -f 1,3,5,6,7,8,9,10,11 -d ';' ./<version_number>_ipv6.csv >> ./<version_number>.csv  
```
7. Upload the file to: `s3://io-flow-location/digitalelement/edge/`
8. Update the `digitalelement.file.uri` property of the `application.production.conf` to point to this new file.

The default value of the property points to a truncated version of the file at `s3://io-flow-location/digitalelement/edge/0508_mini.csv`.  If you want to use a local file you can override the property with the `DIGITALELEMENT_FILE_URI` environment variable using a `file://` uri.

Tests are run against a sample dataset that is committed to VC in `./test/resources/digitalelement_sample.csv`. If you need to test against specific data, be sure to update that file.  (it can also be used as the `DIGITALELEMENT_FILE_URI` if you need to run the app locally without access to the S3 bucket).

If you are running out of memory or having GC issues when trying to build the full index, you probably need to increase SBT's heap size by setting the SBT_OPTS environment var: `SBT_OPTS="-Xms2G"`

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

