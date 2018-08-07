package de.hampager.dapnet.service.database;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
		if (securityContext.getUserPrincipal() == null) {
			return Response.status(Status.FORBIDDEN).build();
		} else if (!username.equalsIgnoreCase(securityContext.getUserPrincipal().getName())
				&& !(securityContext.isUserInRole(UserRoles.ADMIN)
						|| securityContext.isUserInRole(UserRoles.SUPPORT))) {
			return Response.status(Status.FORBIDDEN).build();
		}

		JsonObject obj = client.get(username);
		if (obj != null) {
			return Response.ok(obj).build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

}
