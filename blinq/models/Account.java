package com.blinq.models;

/**
 * Created by Osama on 8/10/2014.
 */

import com.blinq.HeadboxAccountsManager;

/**
 * Value type that represents an Account in the {@link com.blinq.HeadboxAccountsManager}. This object overrides
 * {@link #equals} and {@link #hashCode}, making it
 * suitable for use as the key of a {@link java.util.Map}
 */
public class Account {

    public final String name;
    public final HeadboxAccountsManager.AccountType type;
    public final String accessToken;


    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Account)) return false;
        final Account other = (Account) o;
        return name.equals(other.name) && type.equals(other.type);
    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    public Account(String name, HeadboxAccountsManager.AccountType type, String accessToken) {

        this.name = name;
        this.type = type;
        this.accessToken = accessToken;
    }


    public String toString() {
        return "Account {name=" + name + ", type=" + type + "}";
    }
}