package model;
abstract class Entity{
    private String id;
    public abstract Entity(String id);
    public abstract void setId(String id);
    public abstract void getId();
}