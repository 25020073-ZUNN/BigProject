package model;
public abstract class User extends Entity {
    protected String name;
    protected String email;

    public User(String id, String name, String email) {
        super(id);
        this.name = name;
        this.email = email;
    }
    public String getName() {
        return name;
    }
}
