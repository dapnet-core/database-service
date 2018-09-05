package de.hampager.dapnet.service.database;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import de.hampager.dapnet.service.database.model.LoginResponse;
import de.hampager.dapnet.service.database.model.LoginUser;
import de.hampager.dapnet.service.database.model.PermissionValue;

public class AppUser implements UserDetails {

	private static final long serialVersionUID = 1L;
	private final String username;
	private final String revision;
	private final String email;
	private final boolean enabled;
	private final Instant createdOn;
	private final String createdBy;
	private final Map<String, PermissionValue> permissions;
	private final Collection<GrantedAuthority> authorities;

	public AppUser(LoginResponse login) {
		final LoginUser user = login.getUser();
		this.username = user.getId();
		this.revision = user.getRevision();
		this.email = user.getEmail();
		this.enabled = user.isEnabled();
		this.createdBy = user.getCreatedBy();
		this.createdOn = user.getCreatedOn();
		this.permissions = Collections.unmodifiableMap(login.getPermissions());
		this.authorities = convertToAuthorities(user.getRoles());
	}

	public String getRevision() {
		return revision;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Map<String, PermissionValue> getPermissions() {
		return permissions;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	private static Collection<GrantedAuthority> convertToAuthorities(Set<String> roles) {
		final Set<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
				.collect(Collectors.toSet());
		return Collections.unmodifiableSet(authorities);
	}

}
