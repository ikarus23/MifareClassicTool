package org.t246osslab.easybuggy.core.model;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable{
    
    private static final long serialVersionUID = 1L;
    private String userId = null;
    private String name = null;
    private String password = null;
    private String secret = null;
    private String phone = null;
    private String mail = null;
    private int loginFailedCount = 0;
    private Date lastLoginFailedTime = null;

    @Override
    public String toString() {
        return "User [userId=" + userId + ", name=" + name + ", password=" + password + ", secret=" + secret
                + ", phone=" + phone + ", mail=" + mail + ", loginFailedCount=" + loginFailedCount
                + ", lastLoginFailedTime=" + lastLoginFailedTime + "]";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public int getLoginFailedCount() {
        return loginFailedCount;
    }

    public void setLoginFailedCount(int loginFailedCount) {
        this.loginFailedCount = loginFailedCount;
    }

    public Date getLastLoginFailedTime() {
        return lastLoginFailedTime;
    }

    public void setLastLoginFailedTime(Date lastLoginFailedTime) {
        this.lastLoginFailedTime = lastLoginFailedTime;
    }
}
