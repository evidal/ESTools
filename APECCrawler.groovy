import groovy.json.StringEscapeUtils
import groovy.json.JsonBuilder
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.2.2')
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPost
import groovy.time.*
import java.util.Random
import java.text.DecimalFormat
import org.apache.http.impl.client.DefaultHttpClient


def rootURL = "http://cadres.apec.fr"
def apecRootPage = rootURL+"/liste-offres-emploi-cadres/9_0__________offre-d-emploi.html"
apecRootPage = rootURL+"/liste-offres-emploi-cadres/5_1314__________offre-d-emploi.html"
def page = new URL(apecRootPage).getText()
def annonce =[:]
def client = new DefaultHttpClient()

while(page != null) {
	// <a href="/offres-emploi-cadres/0_0_0_38381658W________offre-d-emploi-ingenieur-specialise-en-risques-industriels-h-f-h-f.html">
	// </a>

	try {
		page.findAll(~/<a.*href="(\/offres-emploi-cadres\/.*)".*>/) { link ->
			try {
				println "URL job : "+link[1]		
				
				// ID = URL
				annonce.identity = rootURL+link[1]		
				// source
				annonce.source = "APEC"
				
				// Follow the link
				def content = new URL(rootURL+link[1]).getText()
				
				content.find(~/<h1 class="detailOffre">Détail de l'offre :(.*)<\/h1>/) { 
					// JobName
					annonce.jobName = it[1].trim()
				}
				
				// Enterprise
			  	content.findAll(~/<th valign="top">Société :<\/th>\s*<td>\s*(<img\s*src='.*'\s*alt='.*'\s*\/>\s*<br \/>\s*)?(.*)/) {
					  annonce.company = it[2].trim()
				}		
				
				// Location
				content.find(~/<tr>\s*<th>Lieu :<\/th>\s*<td>(.*)<\/td>\s*<\/tr>/) {
					annonce.location = it[1].trim()
				}
				
				// Description
				content.find(~/(?ms)<div class="contentWithDashedBorderTop marginTop boxContent">\s*<div class="boxContentInside">\s*<p>(.*?)<\/p>.*<\/div>/) {
					annonce.content = it[1].replaceAll(~/<\/?\w*>/, " ").trim()
				}
				
				// Date
				content.find(~/<tr>\s*<th>Date de publication :<\/th>\s*<td>(.*)<\/td>\s*<\/tr>/) {
					annonce.date = it[1].trim()
				}
				
				// Salary
				content.find(~/<tr>\s*<th>Salaire :<\/th>\s*<td>(.*)<\/td>\s*<\/tr>/) {
					annonce.salary = it[1].trim()
				}
				
				// contract
				content.find(~/(?s)<tr>\s*<th style="width: 110px;">Type de contrat :<\/th>\s*<td>(.*?)\s*<\/td>\s*<\/tr>/) {
					annonce.contract = it[1].trim()
				}
				
				// Experience
				content.find(~/<tr>\s*<th>Expérience :<\/th>\s*<td>(.*)<\/td>\s*<\/tr>/) {
					annonce.experience = it[1].trim()
				}
				
				
				def json = new JsonBuilder(annonce)
				dvc = new HttpPost('http://localhost:9200/meta_jobs/jobs')
				def ent =  new StringEntity(json.toString());
				dvc.setEntity(ent)
				def response = client.execute(dvc)
				response.getStatusLine().getStatusCode()
				response.getEntity().getContent().readLines()
			} catch(Exception e) {
				e.printStackTrace()
			}
		}	
	} catch(Exception e) {
		e.printStackTrace()	
	}
	
	
	// Next page
	// <a href="/liste-offres-emploi-cadres/5_1__________offre-d-emploi.html" class="lastItem">Suivante</a>
	def nextPageLink = page =~ /<a.*href="(\/liste-offres-emploi-cadres\/.*html)".*class="lastItem">Suivante<\/a>/
	if(nextPageLink != null) {
		println "URL next page : "+nextPageLink[0][1]
		page = new URL("http://cadres.apec.fr"+nextPageLink[0][1]).getText()
	} else {
		println "no next link"
		page = null
	}
}