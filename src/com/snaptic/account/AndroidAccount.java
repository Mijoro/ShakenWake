package com.snaptic.account;

public class AndroidAccount {
  private String authToken;
  private long id;
  private String email;
  private String username;
  private String password;
  private long created_on;
  private String logintype;
  private long lastSyncId;

  public String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(String token) {
    this.authToken = token;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
	    return password;
	  }

	  public void setPassword(String password) {
	    this.password = password;
	  }
	  
  public long getCreatedOn() {
	  return created_on;
  }

  public void setCreatedOn(long created_on) {
	  this.created_on = created_on;
  }

  public long getId() {
	  return id;
  }

  public void setId(long id) {
	  this.id = id;
  }
  
  public String getLoginType() {
	  return logintype;
  }

  public void setLoginType(String logintype) {
	  this.logintype = logintype;
  }
  
  public boolean isValid() {
    return (authToken != null && authToken.length() > 0);
  }
  
  public long getLastSyncId() {
	  return lastSyncId;
  }
  
  public void setLastSyncId(long lastSyncId) {
	  this.lastSyncId = lastSyncId;
  }

  @Override
  public String toString() {
    return "id: " + id + " username: " + username + " password: "+ password + " auth_token: " + authToken + " email: " + email + " created_on: " + created_on + " logintype: " + logintype;
  }
}
