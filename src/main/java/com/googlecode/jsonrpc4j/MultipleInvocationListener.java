package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link com.googlecode.jsonrpc4j.InvocationListener} that supports the use
 * of multiple {@link InvocationListener}s called one after another.
 *
 * @author Andrew Lindesay
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class MultipleInvocationListener implements InvocationListener {
	
	private final List<InvocationListener> invocationListeners;
	
	/**
	 * Creates with the given {@link InvocationListener}s,
	 * {@link #addInvocationListener(InvocationListener)} can be called to
	 * add additional {@link InvocationListener}s.
	 *
	 * @param invocationListeners the {@link InvocationListener}s
	 */
	public MultipleInvocationListener(InvocationListener... invocationListeners) {
		this.invocationListeners = new LinkedList<>();
		Collections.addAll(this.invocationListeners, invocationListeners);
	}
	
	/**
	 * Adds an {@link InvocationListener} to the end of the
	 * list of invocation listeners.
	 *
	 * @param invocationListener the {@link InvocationListener} to add
	 */
	public void addInvocationListener(InvocationListener invocationListener) {
		this.invocationListeners.add(invocationListener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void willInvoke(Method method, List<JsonNode> arguments) {
		for (InvocationListener invocationListener : invocationListeners) {
			invocationListener.willInvoke(method, arguments);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void didInvoke(Method method, List<JsonNode> arguments, Object result, Throwable t, long duration) {
		for (InvocationListener invocationListener : invocationListeners) {
			invocationListener.didInvoke(method, arguments, result, t, duration);
		}
	}
	
}
