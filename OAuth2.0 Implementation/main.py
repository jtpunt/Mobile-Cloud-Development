# from google.appengine.ext.webapp import template
from google.appengine.api import urlfetch
import functools
import logging
import jinja2
import webapp2
import urllib
import json
import os

CLIENT_ID = '383557217781-i5n3bcrjprb6evjfjhne25tmo1mp0ok5.apps.googleusercontent.com'
CLIENT_SECRET = 'IvLJxuGBs1NlGw5MyonNxJEe'
REDIRECT_URI = 'https://oauthimplementation-210823.appspot.com/oauth'

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__) + '/views'),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True
)
# This class handles HTTP verbs requesting '/' routes
class MainPage(webapp2.RequestHandler):
    # HTTP Get Request that sends the end user to the OAuth Provider, where the user will interact with the server to
    # let it know that it authorizes the client to access its protected resource.
    def get(self):
        def buildURL():
            return 'https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=' + CLIENT_ID + \
                    '&redirect_uri=' + REDIRECT_URI + '&scope=email&state=SuperSecret9000'
        template = JINJA_ENVIRONMENT.get_template('index.html')
        self.response.write(template.render({'url': buildURL()}))

class OAuthHandler(webapp2.RequestHandler):
    def get(self):
        code = self.request.get('code') # Returns values for arguments parsed from the query
        state = self.request.get('state')
        def handle_result(rpc):
            try: # Try to return the result object from the RPC Object's get_result() method
                result = rpc.get_result() # returns the result object 
                if result.status_code == 200: # Did the server respond with 200 - OK?
                    token = json.loads(result.content) # Deserializes JSON to an object - our token
                    header = {'Authorization': 'Bearer ' + token['access_token']}
                    try: # try
                        response = urlfetch.fetch("https://www.googleapis.com/plus/v1/people/me", 
                            method = urlfetch.GET, 
                            headers = header
                        )
                        gplus_resp = json.loads(response.content) 
                        template_values = {'username': gplus_resp['displayName'],'email': gplus_resp['emails'][0]['value']}
                        template = JINJA_ENVIRONMENT.get_template('oauth.html')
                        self.response.write(template.render(template_values))
                    except urlfetch.Error:
                        logging.exception('Caught exception fetching url')
                else:
                    self.response.status_int = result.status_code
                    self.response.write('URL returned status code {}'.format(result.status_code))
            except urlfetch.DownloadError: # an error occured returning the result object from the RPC Object's get_result() method
                self.response.status_int = 500
                self.response.write('Error fetching URL')


                # The client needs to verify the state matches one that was sent previously
        if not state or state != 'SuperSecret9000':
            self.response.status = 500
            self.response.write("ERROR: State did not match what was previously sent.")
        else:
                # Setting up asynchronous token request using a callback function
            rpc = urlfetch.create_rpc() # Create a new RPC object - which represents your asychronous call in subsequent method calls
            rpc.callback = functools.partial(handle_result, rpc) # Set the callback function for the RPC object
            post_body = {
                'code': code,
                'client_id': CLIENT_ID,
                'client_secret': CLIENT_SECRET,
                'redirect_uri': REDIRECT_URI,
                'grant_type': 'authorization_code'
            }
            payload = urllib.urlencode(post_body)
            header = {'Content-Type':'application/x-www-form-urlencoded'}

            # Request the access token
            urlfetch.make_fetch_call(rpc, 
                url = 'https://www.googleapis.com/oauth2/v4/token', 
                payload = payload, 
                method = urlfetch.POST
            )
            rpc.wait() # wait for the handle_result callback to complete
            logging.info('Done waiting for RPCs')


app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/oauth', OAuthHandler)
], debug=True)