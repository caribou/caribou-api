# Caribou API

This module creates an HTTP API based on the models defined by the database
given in the database configuration.  

## Usage

Fire up the API and navigate to it: <pre>http://localhost:33443</pre><br />
Look at all the models: <pre>http://localhost:33443/models</pre><br />
Include the fields: <pre>http://localhost:33443/models?include=fields</pre><br />
And their links: <pre>http://localhost:33443/models?include=fields.link</pre><br />
Order by the field slugs: <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc</pre><br />
Limit to 3:  <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3</pre><br />
Only include models who have a "Name" field:  <pre>http://localhost:33443/models?include=fields.link&order=fields.slug%20desc&limit=3&where=fields.name:Name</pre><br />

## License

Copyright (C) 2012 Instrument

Distributed under the MIT License.
