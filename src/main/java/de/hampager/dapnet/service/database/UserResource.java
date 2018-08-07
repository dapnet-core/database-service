package de.hampager.dapnet.service.database;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Singleton
@Path("users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class UserResource extends AbstractResource {

	@Inject
	private UserClient client;

	@GET
	@RolesAllowed({ UserRoles.ADMIN, UserRoles.SUPPORT })
	public Response getAll() {
		JsonObject obj = client.getAll(true);
		return Response.ok(obj).build();
	}

//	@GET
//	@RolesAllowed({ UserRoles.ADMIN, UserRoles.SUPPORT })
//	public Response getRangeByNames(@QueryParam("startkey") String startKey, @QueryParam("endkey") String endKey) {
//		JsonObject obj = client.get(startKey, endKey, true);
//		return Response.ok(obj).build();
//	}

	@GET
	@Path("_usernames")
	public Response getAllNames() {
		JsonObject obj = client.getAll(false);
		return Response.ok(obj).build();
	}

	@GET
	@Path("{username}")
	public Response get(@PathParam("username") String username) {
		if (!isAuthenticated()) {
			return Response.status(Status.FORBIDDEN).build();
		} else if (!isCurrentUser(username) && !(isUserInRole(UserRoles.ADMIN) || isUserInRole(UserRoles.SUPPORT))) {
			return Response.status(Status.FORBIDDEN).build();
		}

		JsonObject obj = client.get(username);
		if (obj != null) {
			return Response.ok(obj).build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@PUT
	@RolesAllowed({ UserRoles.ADMIN, UserRoles.SUPPORT })
	public Response add(JsonObject user) {
		return Response.ok().build();
	}

	@DELETE
	@Path("{username}")
	public Response delete(@PathParam("username") String username, @QueryParam("rev") String revision) {
		if (!isAuthenticated()) {
			return Response.status(Status.FORBIDDEN).build();
		} else if (!isCurrentUser(username) && !(isUserInRole(UserRoles.ADMIN) || isUserInRole(UserRoles.SUPPORT))) {
			return Response.status(Status.FORBIDDEN).build();
		}

		// TODO Delete all related stuff

		if (client.delete(username, revision)) {
			return Response.ok().build();
		} else {
			return Response.notModified().build();
		}
	}

}
