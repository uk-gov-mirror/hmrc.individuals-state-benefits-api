individuals-state-benefits-api
========================

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The Individuals State Benefits API allows a developer to:                                                      
- list and retrieve state benefits
- create or amend customer added state benefits
- delete customer added state benefits
- create or amend benefits financial data
- mark a state benefit as ignored

For certain scenarios, a request may not be made for a tax year which has not yet ended. This includes:
- creating, or amending a customer added state benefit
- ignoring a HMRC held state benefit
- amending state benefit amounts for benefit types which are not Job Seekers Allowance (JSA) or Employment Support Allowance (ESA)

## Requirements
- Scala 2.12.x
- Java 8
- sbt 1.3.13
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup
Run the microservice from the console using: `sbt run` (starts on port 7789 by default)

Start the service manager profile: `sm --start MTDFB_INDIVIDUALS_STATE_BENEFITS`
 
## Run Tests
Run unit tests: `sbt test`

Run integration tests: `sbt it:test`

## To view the RAML
To view documentation locally, ensure the Individuals State Benefits API is running, and run api-documentation-frontend:

```
./run_local_with_dependencies.sh
```

Then go to http://localhost:9680/api-documentation/docs/preview and enter the full URL path to the RAML file with the appropriate port and version:

```
http://localhost:7789/api/conf/1.0/application.raml
```

## Reporting Issues
You can create a GitHub issue [here](https://github.com/hmrc/individuals-state-benefits-api/issues)

## API Reference / Documentation 
Available on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-state-benefits-api/1.0)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")