# Name: Jonathan Perry
# Date: 7/14/2018
# Class: CS 496
# Project: REST Planning and Implementation
from google.appengine.ext import ndb
import webapp2
import json

class Boat(ndb.Model):
	id = ndb.StringProperty()
	name = ndb.StringProperty(required=True)
	type = ndb.StringProperty(required=True)
	length = ndb.IntegerProperty(required=True)
	at_sea = ndb.BooleanProperty()

class Slip(ndb.Model):
	id = ndb.StringProperty()
	number = ndb.IntegerProperty(required=True)
	current_boat = ndb.StringProperty(required=True)
	arrival_date = ndb.StringProperty(required=True)
	departure_history = ndb.StringProperty(repeated=True)

# Returns false if the boat_data object contains invalid input
def checkBoatInput(self, boat_data):
	# check to see if type is equal to Catamaran, power boat, sailboat
	if not boat_data['type'] or (boat_data['type'] != "Catamara" and boat_data['type'] != "Power Boat" and boat_data['type'] != "Sailboat"):
		self.response.status = 400
		self.response.write("Error: Invalid Boat Type Input Given!")
		return False
	elif not boat_data['name']:
		self.response.status = 400
		self.response.write("Error: No Boat Name Input Given!")
		return False
	elif not boat_data['length']:
		self.response.status = 400
		self.response.write("Error: No Length Input Given!")
		return False
	else:
		return True

# Returns false if the slip_data object doesn't contain the key 'number'
def checkSlipInput(self, slip_data):
	if not slip_data['number']:
		self.response.status = 400
		self.response.write("Error: No Slip Number Input Given!")
		return False
	else:
		return True

# Returns true if the boat id we are looking for is in the databse
def checkBoatID(self, id):
	for boat in Boat.query():
		if boat.id == id:
			return True
	self.response.write("Error: Boat ID not found!")
	self.response.status = 400
	return False

# Returns true if the slip id we are looking for is in the database
def checkSlipID(self, id):
	for slip in Slip.query():
		if slip.id == id:
			return True
	self.response.write("Error: Slip ID not found!")
	self.response.status = 400
	return False

# Returns true if any slips contain the boat we are looking for
def checkBoatInSlip(self, boat_id):
	for slip in Slip.query(): # find which slip is associated with this boat
		if slip.current_boat == boat_id:
			return True
	return False

# Returns False if a slip has already has the number we are searching for
def checkSlipNumber(self, number):
	for slip in Slip.query():
		if slip.number == number:
			self.response.status = 403;
			self.response.write("Error: That slip number is already in use!")
			return False
	return True

# This class handles HTTP verbs requesting both /boats and /boats/(.*) routes
class BoatHandler(webapp2.RequestHandler):
	# 1.2 Delete a boat
	def delete(self, id=None):
		# Make sure an id was sent, and that the boat exists in the database
		if id and checkBoatID(self, id):
			# 1.2.1 deleting a ship should empty the slip the boat was previously in
			for slip in Slip.query(): # find which slip is associated with this boat
				if slip.current_boat == id:
					slip.current_boat = ""
			ndb.Key(urlsafe=id).delete()
			self.response.write("Success: Boat has been deleted")

	# 1.4 Getting a single ship or all ships
	def get(self, id=None):
		# Make sure an id was sent, and that the boat exists in the database
		if id and checkBoatID(self, id): # 1.4.1 You should be able to view the details of a single boat
			my_boat = ndb.Key(urlsafe=id).get() # get the boat from the db that we want to display
			my_boat_dict = my_boat.to_dict() # convert the boat to a dictionary
			my_boat_dict['self'] = "/boats/" + id # 1.4.2 it should be possible, via a url, to view a specific boat
			self.response.write(json.dumps(my_boat_dict))
		elif not id: # 1.4.1 You should be able to get a list of all boats
			my_boat_results = [my_boat.to_dict() for my_boat in Boat.query()]
			for boat in my_boat_results:
				boat['self'] = "/boats/" + boat['id']
			self.response.write(json.dumps(my_boat_results))

	# 2. Setting a boat to be "at sea"
	def patch(self, id=None):
		if id and checkBoatID(self, id):
			my_boat	= ndb.Key(urlsafe=id).get() # get the boat from the db that we want to put "at_sea"
			if my_boat.at_sea: # is the boat "at_sea" already?
				self.response.status = 400
				self.response.write("Error: Boat is already at sea!")
			else: # boat is at slip
				my_boat.at_sea = True # 2.3 put the boat "at_sea"
				my_boat.put()
				slip_found = False # boat may be associated with a slip
				for slip in Slip.query(): # find which slip is associated with this boat
					if slip.current_boat == my_boat.id:
						slip_found = True
						slip.current_boat = "" # 2.1 This should cause the previously occupied slip to become empty
						slip.put()
				if slip_found:
					self.response.write("Success: Boat is at sea and the previously occupied slip is now empty")
				else:
					self.response.write("Success: Boat is at sea, but no previously occupied slip was found")

	# 1.1 Add a boat
	def post(self): 
		boat_data = json.loads(self.request.body)
		if checkBoatInput(self, boat_data): # is the input sent in the body valid?
			new_boat = Boat(name=boat_data['name'], type=boat_data['type'], length=boat_data['length']) # create boat object
			new_boat.at_sea = True # All newly created boats should start "At sea"
			new_boat.put() # Persist the entity to the cloud datastore
			new_boat.id = new_boat.key.urlsafe() # assign the entity's key to the id of the boat object
			new_boat.put() # Persist the entity to the cloud datastore
			new_boat_dict = new_boat.to_dict() # convert to a dictionary
			new_boat_dict['self'] = '/boats/' + new_boat.id # add a url to view this boat later on
			self.response.write(json.dumps(new_boat_dict)) 

	# 1.3 Modify a boat
	def put(self, id=None):
		boat_data = json.loads(self.request.body)
		# Make sure an id was sent, the boat exists in the db, and that the boat input sent over is valid
		if len(boat_data) == 3: # only the name, type, and length attributes can be updated
			if id and checkBoatID(self, id) and checkBoatInput(self, boat_data):
				my_boat = ndb.Key(urlsafe=id).get()
				my_boat.name = boat_data['name']
				my_boat.type = boat_data['type']
				my_boat.length = boat_data['length']
				my_boat.put() # Persist the entity to the cloud datastore
				self.response.write("Success: Boat has been edited")
		else: # Too many arguments sent in to update the boat
			self.response.status = 400
			self.response.write("Error: 3 arguments needed to modify a boat")

# This class handles HTTP verbs requesting /slips nad /slips/(.*) routes
class SlipHandler(webapp2.RequestHandler):
	# 1.2 Delete a slip
	def delete(self, id=None):
		# Make sure an id was sent and that the slip id exists in the database
		if id and checkSlipID(self, id):
			delete_slip = ndb.Key(urlsafe=id).get() # get the slip we need to delete
			# 1.2.2 Deleting a pier a boat is currently in should set the boat to be "at sea"
			if delete_slip.current_boat: # Is the slip assigned to some boat?
				current_boat = ndb.Key(urlsafe=delete_slip.current_boat).get() # get the boat associated with the slip
				current_boat.at_sea = True # set the boat to be at sea
				current_boat.put() # update the boat in the database
			delete_slip.key.delete() # delete the slip
			self.response.write("Success: Slip has been deleted")

	# 1.4 View a single slip or all slips
	def get(self, id=None):
		# Make sure an id was sent and that the slip id exists in the database
		if id and checkSlipID(self, id): # 1.4.1 You should be able to view the details of a single slip
			my_slip = ndb.Key(urlsafe=id).get() # get the slip that we want to display
			my_slip_dict = my_slip.to_dict() # convert the slip to a dictionary
			my_slip_dict['self'] = '/slips/' + id # 1.4.2 it should be possible, via a url, to view a specific boat occupying any slip
			self.response.write(json.dumps(my_slip_dict))
		elif not id: # 1.4.1 You should be able to get a list of all slips
			my_slip_results = [my_slip.to_dict() for my_slip in Slip.query()]
			for slip in my_slip_results:
				slip['self'] = "/slips/" + slip['id']
			self.response.write(json.dumps(my_slip_results))

	# 1.3 Modify a slip number
	def patch(self, id=None): 
		slip_data = json.loads(self.request.body)
		if len(slip_data) == 1: # should only be used to edit the 'number' attribute
			# Make sure an id was sent, the slip input sent over is valid, and that the slip number sent over the body exists in the db
			if id and checkSlipID(self, id) and checkSlipInput(self, slip_data) and checkSlipNumber(self, slip_data['number']):
				my_slip = ndb.Key(urlsafe=id).get() # get the slip from th db
				my_slip.number = slip_data['number'] # assign the slip number sent over from the body to the slip entity
				my_slip.put() # Persist the entity to the cloud datastore
				self.response.write("Success: Slip has been updated")
		else: # Too many arguments sent in over body
			self.response.status = 400
			self.response.write("Error: Number is the only field that can be changed")

	# 1.1 Add a slip
	def post(self): 
		slip_data = json.loads(self.request.body)
		# Nake sure that the slip input sent in the body is valid and check to see if slip # sent in the body has already been used
		if checkSlipInput(self, slip_data) and checkSlipNumber(self, slip_data['number']):
			new_slip = Slip(number=slip_data['number']) # Create the slip object
			new_slip.current_boat = "" # 1.1.2 All newly created slips should be empty
			new_slip.arrival_date = "" # 1.1.2 ALl newly created slips should be empty
			new_slip.put() # Persist entity to the cloud datastore
			new_slip.id = new_slip.key.urlsafe() # assign the entities generated key value to the id of the slip object
			new_slip.put() # Persist entity to the cloud datastore
			new_slip_dict = new_slip.to_dict() # convert slip object to dictionary
			new_slip_dict['self'] = '/slips/' + new_slip.id # add a url to this slip so we can view this slip later
			self.response.write(json.dumps(new_slip_dict))

# This class handles HTTP verbs requesting both /slips/:slipID/boat routes
class SlipBoatHandler(webapp2.RequestHandler):
	# 1.4.2 It should be possible, via a url, to view the specific boat currently occupying any slip
	def get(self, id=None):
		if id and checkSlipID(self, id):
			my_slip = ndb.Key(urlsafe=id).get() 
			if my_slip.current_boat:
				my_boat = ndb.Key(urlsafe=my_slip.current_boat).get()
				my_boat_dict = my_boat.to_dict()
				my_boat_dict['self'] = "/boats/" + my_boat.current_boat
				self.response.write(json.dumps(my_boat_dict))
			else:
				self.response.write("Error: No boat associated with the specified slip")

	# 3. Managing a boat arival
	def put(self, id=None):
		boat_data = json.loads(self.request.body)
		# should not be more than 2 arguments sent in
		if len(boat_data) == 2:
			# Make sure an id was sent, and both the slip id and boat id exist in the database
			if id and checkSlipID(self, id) and checkBoatID(self, boat_data['current_boat']):
				my_slip = ndb.Key(urlsafe=id).get() # get the slip from db
				my_boat = ndb.Key(urlsafe=boat_data['current_boat']).get() # get boat from database
				if not my_slip.current_boat:
					if my_boat.at_sea: # is the boat already at sea? or slip already occupied?
						my_slip.current_boat = boat_data['current_boat'] # 3.1 A ship should be assigned a slip #
						my_slip.arrival_date = boat_data['arrival_date'] # 3.1 A ship should be able to arrive
						my_slip.put() # update database
						my_boat.at_sea = False # boat is no longer at see
						my_boat.put() # update database
						self.response.write("Success: Boat was added to the slip and is no longer in the sea")
					else: # 3.2 if the slip is occupied the server should return an Error 403 Forbidden message
						self.response.status = 403 
						self.response.write("Error: Boat is currently in a slip!")
				else:
					self.response.status = 403 
					self.response.write("Error: That slip is already occupied!")
		else:
			self.response.status = 400
			self.response.write("Error: Only 2 arguments needed to manage a boat arrival!")

class MainPage(webapp2.RequestHandler):
    def get(self):
        self.response.write('Hello, Gets!')


allowed_methods = webapp2.WSGIApplication.allowed_methods
new_allowed_methods = allowed_methods.union(('PATCH',))
webapp2.WSGIApplication.allowed_methods = new_allowed_methods

app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/boats', BoatHandler),
    ('/boats/(.*)', BoatHandler), # handles everything after /boat/
    ('/slips', SlipHandler),
    ('/slips/(.*)/boat', SlipBoatHandler),
    ('/slips/(.*)', SlipHandler)
], debug=True)
