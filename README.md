# Caribou API

This module creates an HTTP API based on the models defined by the database
given in the database configuration.  

## Usage

Fire up the API and navigate to http://localhost:33443
Look at all the models:  http://localhost:33443/models
Include the fields:  http://localhost:33443/models?include=fields
And their links:  http://localhost:33443/models?include=fields.link
Order by the field slugs:  http://localhost:33443/models?include=fields.link&order=fields.slug%20desc
Limit to 3:  http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3
Only include models who have a "Name" field:  http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3&where=fields.name:Name

## License

Copyright (C) 2012 Instrument

Distributed under the MIT License.
