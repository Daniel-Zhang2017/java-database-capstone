package com.project.back_end.DTO;

public class Login {
    
    // 1. 'identifier' field:
    //    - Type: private String
    //    - Description:
    //      - Represents the unique identifier of the user attempting to log in.
    //      - For Doctor/Patient users, this will be their email address.
    //      - For Admin users, this will be their username.
    private String identifier;

    // 2. 'password' field:
    //    - Type: private String
    //    - Description:
    //      - Represents the password associated with the user's account.
    //      - The password field is used for verifying the user's identity during login.
    //      - It should be transmitted securely and hashed before being stored or compared during authentication.
    private String password;

    // 3. Default Constructor:
    //    - The default no-argument constructor is provided for compatibility with frameworks.
    //    - This allows the class to be instantiated via reflection during deserialization.
    public Login() {
    }

    // 3.1. Parameterized Constructor:
    //    - An optional constructor that accepts both identifier and password.
    //    - This can be useful for creating Login objects programmatically.
    public Login(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // 4. Getters and Setters:
    //    - Standard getter and setter methods are provided for both 'identifier' and 'password' fields.
    //    - These methods enable proper deserialization of the login request body.

    // Getter for identifier
    public String getIdentifier() {
        return identifier;
    }

    // Setter for identifier
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    // Getter for password
    public String getPassword() {
        return password;
    }

    // Setter for password
    public void setPassword(String password) {
        this.password = password;
    }
}