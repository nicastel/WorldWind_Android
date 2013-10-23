/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.event;

/**
 * General purpose message event.
 * 
 * @author pabercrombie
 * @version $Id: Message.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class Message extends WWEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2961985628983809212L;
	/** Message name. */
	protected String name;
	/** Time at which the message was sent. */
	protected long when;

	/**
	 * Create a message. The message will be timestamped with the current system time.
	 * 
	 * @param name
	 *            The name of the message.
	 * @param source
	 *            The object that generated the message.
	 */
	public Message(String name, Object source) {
		this(name, source, System.currentTimeMillis());
	}

	/**
	 * Create a message, with a timestamp.
	 * 
	 * @param name
	 *            The name of the message.
	 * @param source
	 *            The object that generated the message.
	 * @param when
	 *            The timestamp to apply to the message.
	 */
	public Message(String name, Object source, long when) {
		super(source);
		this.name = name;
		this.when = when;
	}

	/**
	 * Indicates the message name.
	 * 
	 * @return The message name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Indicates the time at which the message was sent.
	 * 
	 * @return Time, in milliseconds since the Epoch, at which the message was sent.
	 */
	public long getWhen() {
		return this.when;
	}
}