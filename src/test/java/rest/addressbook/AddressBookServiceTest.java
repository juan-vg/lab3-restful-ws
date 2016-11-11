package rest.addressbook;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import rest.addressbook.config.ApplicationConfig;
import rest.addressbook.domain.AddressBook;
import rest.addressbook.domain.Person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * A simple test suite
 *
 */
public class AddressBookServiceTest {

    private HttpServer server;

    @Test
    public void serviceIsAlive() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        launchServer(ab);

        // Get number of contacts before requests
        int ABSizeBefore = ab.getPersonList().size();

        // First request to the address book
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts").request().get();
        assertEquals(200, response.getStatus());

        // Get the number of contacts from the first request
        int req1ABSize = response.readEntity(AddressBook.class).getPersonList().size();

        // Second request to the address book
        response = client.target("http://localhost:8282/contacts").request().get();
        assertEquals(200, response.getStatus());

        // Get the number of contacts from the second request
        int req2ABSize = response.readEntity(AddressBook.class).getPersonList().size();

        // Get number of contacts after request
        int ABSizeAfter = ab.getPersonList().size();

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts is well implemented by the service
        //////////////////////////////////////////////////////////////////////

        // test that it is safe
        assertEquals(ABSizeBefore, ABSizeAfter);

        // test that it is idempotent
        assertEquals(req1ABSize, req2ABSize);
    }

    @Test
    public void createUser() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        launchServer(ab);

        // Get the number of contacts before requests
        int ABSizeBefore = ab.getPersonList().size();

        // Prepare data
        Person juan = new Person();
        juan.setName("Juan");
        URI juanURI1 = URI.create("http://localhost:8282/contacts/person/1");
        URI juanURI2 = URI.create("http://localhost:8282/contacts/person/2");

        // Create the first new user
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts").request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(juan, MediaType.APPLICATION_JSON));

        assertEquals(201, response.getStatus());
        assertEquals(juanURI1, response.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person juanUpdated1 = response.readEntity(Person.class);
        assertEquals(juan.getName(), juanUpdated1.getName());
        assertEquals(1, juanUpdated1.getId());
        assertEquals(juanURI1, juanUpdated1.getHref());

        // Check that the first new user exists
        response = client.target("http://localhost:8282/contacts/person/1").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        juanUpdated1 = response.readEntity(Person.class);
        assertEquals(juan.getName(), juanUpdated1.getName());
        assertEquals(1, juanUpdated1.getId());
        assertEquals(juanURI1, juanUpdated1.getHref());

        // Get the URI from the first new user
        URI req1URI = juanUpdated1.getHref();

        // Get the number of contacts between requests
        int ABSizeBetween = ab.getPersonList().size();

        // Create the second new user (same as first new user)
        client = ClientBuilder.newClient();
        response = client.target("http://localhost:8282/contacts").request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(juan, MediaType.APPLICATION_JSON));

        assertEquals(201, response.getStatus());
        assertEquals(juanURI2, response.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person juanUpdated2 = response.readEntity(Person.class);
        assertEquals(juan.getName(), juanUpdated2.getName());
        assertEquals(2, juanUpdated2.getId());
        assertEquals(juanURI2, juanUpdated2.getHref());

        // Check that the second new user exists
        response = client.target("http://localhost:8282/contacts/person/2").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        juanUpdated2 = response.readEntity(Person.class);
        assertEquals(juan.getName(), juanUpdated2.getName());
        assertEquals(2, juanUpdated2.getId());
        assertEquals(juanURI2, juanUpdated2.getHref());

        // Get the URI from the second new user
        URI req2URI = juanUpdated2.getHref();

        // Get the number of contacts after requests
        int ABSizeAfter = ab.getPersonList().size();

        //////////////////////////////////////////////////////////////////////
        // Verify that POST /contacts is well implemented by the service
        //////////////////////////////////////////////////////////////////////

        // test that it is NOT safe
        assertNotEquals(ABSizeBefore, ABSizeBetween);
        assertNotEquals(ABSizeBetween, ABSizeAfter);

        // test that it is NOT idempotent
        assertNotEquals(req1URI, req2URI);
    }

    @Test
    public void createUsers() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        Person salvador = new Person();
        salvador.setName("Salvador");
        salvador.setId(ab.nextId());
        ab.getPersonList().add(salvador);
        launchServer(ab);

        // Prepare data
        Person juan = new Person();
        juan.setName("Juan");
        URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
        Person maria = new Person();
        maria.setName("Maria");
        URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

        // Create a user
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts").request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(juan, MediaType.APPLICATION_JSON));
        assertEquals(201, response.getStatus());
        assertEquals(juanURI, response.getLocation());

        // Create a second user
        response = client.target("http://localhost:8282/contacts").request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(maria, MediaType.APPLICATION_JSON));
        assertEquals(201, response.getStatus());
        assertEquals(mariaURI, response.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person mariaUpdated = response.readEntity(Person.class);
        assertEquals(maria.getName(), mariaUpdated.getName());
        assertEquals(3, mariaUpdated.getId());
        assertEquals(mariaURI, mariaUpdated.getHref());

        // Check that the new user exists
        response = client.target("http://localhost:8282/contacts/person/3").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        mariaUpdated = response.readEntity(Person.class);
        assertEquals(maria.getName(), mariaUpdated.getName());
        assertEquals(3, mariaUpdated.getId());
        assertEquals(mariaURI, mariaUpdated.getHref());

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts/person/3 is well implemented by the
        ////////////////////////////////////////////////////////////////////// service,
        ////////////////////////////////////////////////////////////////////// i.e
        // test that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

    }

    @Test
    public void listUsers() throws IOException {

        // Prepare server
        AddressBook ab = new AddressBook();
        Person salvador = new Person();
        salvador.setName("Salvador");
        Person juan = new Person();
        juan.setName("Juan");
        ab.getPersonList().add(salvador);
        ab.getPersonList().add(juan);
        launchServer(ab);

        // Test list of contacts
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        AddressBook addressBookRetrieved = response.readEntity(AddressBook.class);
        assertEquals(2, addressBookRetrieved.getPersonList().size());
        assertEquals(juan.getName(), addressBookRetrieved.getPersonList().get(1).getName());

        //////////////////////////////////////////////////////////////////////
        // Verify that GET for collections is well implemented by the service,
        ////////////////////////////////////////////////////////////////////// i.e
        // test that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

    }

    @Test
    public void updateUsers() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        Person salvador = new Person();
        salvador.setName("Salvador");
        salvador.setId(ab.nextId());
        Person juan = new Person();
        juan.setName("Juan");
        juan.setId(ab.getNextId());
        URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
        ab.getPersonList().add(salvador);
        ab.getPersonList().add(juan);
        launchServer(ab);

        // Update Maria
        Person maria = new Person();
        maria.setName("Maria");
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts/person/2").request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person juanUpdated = response.readEntity(Person.class);
        assertEquals(maria.getName(), juanUpdated.getName());
        assertEquals(2, juanUpdated.getId());
        assertEquals(juanURI, juanUpdated.getHref());

        // Verify that the update is real
        response = client.target("http://localhost:8282/contacts/person/2").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person mariaRetrieved = response.readEntity(Person.class);
        assertEquals(maria.getName(), mariaRetrieved.getName());
        assertEquals(2, mariaRetrieved.getId());
        assertEquals(juanURI, mariaRetrieved.getHref());

        // Verify that only can be updated existing values
        response = client.target("http://localhost:8282/contacts/person/3").request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
        assertEquals(400, response.getStatus());

        //////////////////////////////////////////////////////////////////////
        // Verify that PUT /contacts/person/2 is well implemented by the
        ////////////////////////////////////////////////////////////////////// service,
        ////////////////////////////////////////////////////////////////////// i.e
        // test that it is idempotent
        //////////////////////////////////////////////////////////////////////

    }

    @Test
    public void deleteUsers() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        Person salvador = new Person();
        salvador.setName("Salvador");
        salvador.setId(1);
        Person juan = new Person();
        juan.setName("Juan");
        juan.setId(2);
        ab.getPersonList().add(salvador);
        ab.getPersonList().add(juan);
        launchServer(ab);

        // Delete a user
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts/person/2").request().delete();
        assertEquals(204, response.getStatus());

        // Verify that the user has been deleted
        response = client.target("http://localhost:8282/contacts/person/2").request().delete();
        assertEquals(404, response.getStatus());

        //////////////////////////////////////////////////////////////////////
        // Verify that DELETE /contacts/person/2 is well implemented by the
        ////////////////////////////////////////////////////////////////////// service,
        ////////////////////////////////////////////////////////////////////// i.e
        // test that it is idempotent
        //////////////////////////////////////////////////////////////////////

    }

    @Test
    public void findUsers() throws IOException {
        // Prepare server
        AddressBook ab = new AddressBook();
        Person salvador = new Person();
        salvador.setName("Salvador");
        salvador.setId(1);
        Person juan = new Person();
        juan.setName("Juan");
        juan.setId(2);
        ab.getPersonList().add(salvador);
        ab.getPersonList().add(juan);
        launchServer(ab);

        // Test user 1 exists
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8282/contacts/person/1").request(MediaType.APPLICATION_JSON)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        Person person = response.readEntity(Person.class);
        assertEquals(person.getName(), salvador.getName());
        assertEquals(person.getId(), salvador.getId());
        assertEquals(person.getHref(), salvador.getHref());

        // Test user 2 exists
        response = client.target("http://localhost:8282/contacts/person/2").request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        person = response.readEntity(Person.class);
        assertEquals(person.getName(), juan.getName());
        assertEquals(2, juan.getId());
        assertEquals(person.getHref(), juan.getHref());

        // Test user 3 exists
        response = client.target("http://localhost:8282/contacts/person/3").request(MediaType.APPLICATION_JSON).get();
        assertEquals(404, response.getStatus());
    }

    private void launchServer(AddressBook ab) throws IOException {
        URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
        server = GrizzlyHttpServerFactory.createHttpServer(uri, new ApplicationConfig(ab));
        server.start();
    }

    @After
    public void shutdown() {
        if (server != null) {
            server.shutdownNow();
        }
        server = null;
    }

}
