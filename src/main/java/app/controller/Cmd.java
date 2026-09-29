package app.controller;


import app.model.physics.Physics;

public interface Cmd {
	
	void execute(Physics model) throws InterruptedException;
}
