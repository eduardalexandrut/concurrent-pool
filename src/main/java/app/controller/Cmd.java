package app.controller;


import app.model.Physics;

public interface Cmd {
	
	void execute(Physics model) throws InterruptedException;
}
