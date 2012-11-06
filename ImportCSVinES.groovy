import groovy.json.JsonBuilder
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.2.2')
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

///////////////////////
// Command Line Parsing
///////////////////////
def url = "localhost:9200"
def type = "defaultindex"
def index = "defaulttype" 

def cli = new CliBuilder(usage: 'ImportCSVinES.groovy -[options] [file]')

cli.with {
	i longOpt: 'index', args:1, 'Elastic search index Name, default "defaultindex"'
	t longOpt: 'type', args:1, 'Elastic search type Name, default "defaulttype"'
	u longOpt: 'URL', args:1, 'Elastic search URL host:port, default "localhost:9200"'
}

def options = cli.parse(args)

if (! options.arguments()) {
	cli.usage()
	return
}

def source = options.arguments()[0]
if(options.i) {
	index = options.i
}
if(options.t) {
	type = options.t
}
if(options.u) {
	url = options.u
}


def client = new DefaultHttpClient()
def response
def input = new File(source);

def fields = []
input.readLines().eachWithIndex { it, i  ->
	if(i == 0) {// Première ligne = Mapping
		it.split(";").each { field ->
			fields.add(field);
		}
	} else 	{ // 2ème lignes et suivantes
		def values = [:]
		it.split(";").eachWithIndex { field, j ->
			values.put(fields[j], field)
		}
		def json = new JsonBuilder(values)
		dvc = new HttpPost('http://'+url+'/'+index+'/'+type)
		println values
		ent =  new StringEntity(json.toString());
		dvc.setEntity(ent)
		response = client.execute(dvc)
		response.getStatusLine().getStatusCode()
		response.getEntity().getContent().readLines()
	}
}