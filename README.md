# Caribou API

This module creates an HTTP API based on the models defined by the database
given in the database configuration.  

## Usage

Fire up the API and navigate to it: <pre>http://localhost:33443</pre>
Look at all the models: <pre>http://localhost:33443/models</pre>
Include the fields: <pre>http://localhost:33443/models?include=fields</pre>
And their links: <pre>http://localhost:33443/models?include=fields.link</pre>
Order by the field slugs: <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc</pre>
Limit to 3:  <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3</pre>
Only include models who have a "Name" field:  <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3&where=fields.name:Name</pre>

## License

Copyright (C) 2012 Instrument

Distributed under the MIT License.
