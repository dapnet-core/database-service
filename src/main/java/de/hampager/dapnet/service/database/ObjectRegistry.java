package de.hampager.dapnet.service.database;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ObjectRegistry<B> implements Iterable<B> {

	private final ConcurrentMap<Class<?>, B> objects = new ConcurrentHashMap<>();

	/**
	 * Registers an object.
	 * 
	 * @param object Object to register
	 * @return Registered object
	 * @throws IllegalArgumentException if the type is already registered.
	 */
	public <T extends B> T put(T object) {
		Objects.requireNonNull(object);
		final B old = objects.putIfAbsent(object.getClass(), object);
		if (old != null) {
			throw new IllegalArgumentException("Type already registered.");
		}

		return object;
	}

	/**
	 * Gets an object from the registry.
	 * 
	 * @param objectType Object type to get.
	 * @return Object or {@code null} if no object of the given type was found.
	 */
	public <T extends B> T get(Class<T> objectType) {
		final B object = objects.get(objectType);
		return objectType.cast(object);
	}

	/**
	 * Removes an object from the registry.
	 * 
	 * @param objectType Object type to remove.
	 * @return Object or {@code null} if no object of the given type was found.
	 */
	public <T extends B> T remove(Class<T> objectType) {
		final B object = objects.remove(objectType);
		return objectType.cast(object);
	}

	/**
	 * Removes all registered objects from the registry.
	 */
	public void clear() {
		objects.clear();
	}

	@Override
	public Iterator<B> iterator() {
		return objects.values().iterator();
	}

}
