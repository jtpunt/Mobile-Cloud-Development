from google.appengine.ext.webapp import template
from google.appengine.api import urlfetch
import webapp2
import urllib
import random
import string
import json
import os

CLIENT_ID = '171611611326-vph1798jdf3u74hu45a2q04tmtpdv0ue.apps.googleusercontent.com'
CLIENT_SECRET = 'faOxj9uBXhYWVt6WGbgoa4CI'
REDIRECT_URI = 'https://oauthimplementation-210823.appspot.com/oauth'

class MainPage(webapp2.RequestHandler):
    def get(self):
        random_string = ''.join([random.choice(string.ascii_letters + string.digits) for n in xrange(32)])

        url_linktext = 'Provide Access'

        url = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=" + CLIENT_ID + \
        "&redirect_uri=" + REDIRECT_URI + "&scope=email&state=" + random_string

        template_values = {'url': url}

        path = os.path.join(os.path.dirname(__file__), 'views/index.html')
        self.response.out.write(template.render(path, template_values))

class OAuthHandler(webapp2.RequestHandler):
    def get(self):
        auth_code = self.request.get('code')
        state = self.request.get('state')
        # Setting up token request
        post_body = {
            'code': auth_code,
            'client_id': CLIENT_ID,
            'client_secret': CLIENT_SECRET,
            'redirect_uri': REDIRECT_URI,
            'grant_type': 'authorization_code'
        }
        payload = urllib.urlencode(post_body)

        # Request the token
        header = {'Content-Type':'application/x-www-form-urlencoded'}
        result = urlfetch.fetch("https://www.googleapis.com/oauth2/v4/token",  # newer versions?
            headers = header,
   		 	payload = payload,
    		method = urlfetch.POST)

        token = json.loads(result.content)

        header = {'Authorization': 'Bearer ' + token['access_token']}
        response = urlfetch.fetch("https://www.googleapis.com/plus/v1/people/me", method = urlfetch.GET, headers=header)

        gplus_data = json.loads(response.content)
        template_values = {'username': gplus_data['displayName'],
                         'email': gplus_data['emails'][0]['value']}
        path = os.path.join(os.path.dirname(__file__), 'views/oauth.html')
        self.response.out.write(template.render(path, template_values))
app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/oauth',OAuthHandler)
], debug=True)