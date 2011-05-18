/*
 * JPAContainer
 * Copyright (C) 2010 Oy IT Mill Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.vaadin.addon.jpacontainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.addon.jpacontainer.testdata.Address;
import com.vaadin.addon.jpacontainer.testdata.Person;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;

/**
 * Test case for {@link JPAContainerItem}.
 * 
 * @author Petter Holmström (IT Mill)
 * @since 1.0
 */
@SuppressWarnings("serial")
public class JPAContainerItemTest {

	private JPAContainerItem<Person> item;
	private Person entity;
	private JPAContainer<Person> container;
	private JPAContainerItem<Person> modifiedItem;
	private String modifiedPropertyId;

	@Before
	public void setUp() {
		container = new JPAContainer<Person>(Person.class) {

			/*
			 * The following two methods are used to verify that the item
			 * notifies the container when items or item properties are changed.
			 * 
			 * The assertions in the beginning of each method make sure that the
			 * test fails if any of the methods are called twice in a row
			 * without the modifiedItem and modifiedPropertyId values being
			 * explicitly set to null in between.
			 */
			@Override
			protected void containerItemPropertyModified(
					JPAContainerItem<Person> item, String propertyId) {
				assertNull("modifiedItem was not null", modifiedItem);
				assertNull("modifiedPropertyId was not null",
						modifiedPropertyId);
				modifiedItem = item;
				modifiedPropertyId = propertyId;
			}

			@Override
			protected void containerItemModified(JPAContainerItem<Person> item) {
				assertNull("modifiedItem was not null", modifiedItem);
				assertNull("modifiedPropertyId was not null",
						modifiedPropertyId);
				modifiedItem = item;
			}
		};
		modifiedItem = null;
		modifiedPropertyId = null;
		container.addNestedContainerProperty("address.street");
		container.addNestedContainerProperty("address.fullAddress");
		entity = new Person();
		entity.setId(123l);
		entity.setAddress(new Address());
		item = new JPAContainerItem<Person>(container, entity);
	}

	@Test
	public void testPrimitiveTypesWithFields() {

		CheckBox checkBox = new CheckBox();
		TextField textField = new TextField();

		EntityItemProperty maleProperty = item.getItemProperty("male");
		EntityItemProperty doubleProperty = item
				.getItemProperty("primitiveDouble");

		checkBox.setPropertyDataSource(maleProperty);
		textField.setPropertyDataSource(doubleProperty);

		Boolean originalMaleValue = (Boolean) checkBox.getValue();
		Double originalDoubleValue = Double.parseDouble((String) textField
				.getValue());

		assertEquals(false, originalMaleValue);
		assertEquals(0.0, originalDoubleValue.doubleValue(), 0.000001);

		checkBox.setValue(true);

		modifiedItem = null;
		modifiedPropertyId = null;

		textField.setValue("3.55");

		modifiedItem = null;
		modifiedPropertyId = null;

		Boolean newMaleValue = (Boolean) checkBox.getValue();
		Double newDoubleValue = Double.parseDouble((String) textField
				.getValue());

		assertEquals(true, newMaleValue);
		assertEquals((Double) 3.55, newDoubleValue);

		Person p = item.getEntity();
		assertEquals(true, p.isMale());
		assertEquals(3.55, p.getPrimitiveDouble(), 0.0000001);
	}

	@Test
	public void testGetItemPropertyIds() {
		Collection<String> propertyIds = item.getItemPropertyIds();
		assertEquals(
				container.getPropertyList().getAllAvailablePropertyNames(),
				propertyIds);
	}

	@Test
	public void testGetItemProperty() {
		assertNotNull(item.getItemProperty("firstName"));
		assertNull(item.getItemProperty("nonexistent"));
	}

	@Test
	public void testIsPersistent() {
		assertTrue(item.isPersistent());
		item.setPersistent(false);
		assertFalse(item.isPersistent());

		// Try the other constructor
		item = new JPAContainerItem<Person>(container, entity, null, true);
		assertFalse(item.isPersistent());
	}

	@Test
	public void testIsDirty_NonPersistent() {
		item.setPersistent(false);
		item.setDirty(true);
		assertFalse(item.isDirty());
	}

	@Test
	public void testGetItemId() {
		assertEquals(123l, item.getItemId());
		// Try the other constructor
		item = new JPAContainerItem<Person>(container, entity, null, true);
		assertNull(item.getItemId());
	}

	@Test
	public void testGetEntity() {
		assertSame(entity, item.getEntity());
	}

	@Test
	public void testGetContainer() {
		assertSame(container, item.getContainer());
	}

	@Test
	public void testPropertyType() {
		assertEquals(String.class, item.getItemProperty("firstName").getType());
		assertEquals(Date.class, item.getItemProperty("dateOfBirth").getType());
		assertEquals(String.class, item.getItemProperty("address.street")
				.getType());
		assertEquals(Address.class, item.getItemProperty("address").getType());

		// should report wrapper types for beans primitive types
		assertEquals(Boolean.class, item.getItemProperty("male").getType());
		assertEquals(Double.class, item.getItemProperty("primitiveDouble")
				.getType());
	}

	@Test
	public void testPropertyReadOnly() {
		assertFalse(item.getItemProperty("firstName").isReadOnly());
		assertFalse(item.getItemProperty("address.street").isReadOnly());
		assertTrue(item.getItemProperty("fullName").isReadOnly());
		assertTrue(item.getItemProperty("address.fullAddress").isReadOnly());
	}

	@Test
	public void testAddNestedProperty() {
		item.addNestedContainerProperty("address.postalCode");
		assertNotNull(item.getItemProperty("address.postalCode"));
	}

	@Test
	public void testRemoveProperty() {
		item.addNestedContainerProperty("address.postalCode");

		assertFalse(item.removeItemProperty("firstName"));
		assertTrue(item.removeItemProperty("address.postalCode"));

		assertNotNull(item.getItemProperty("firstName"));
		assertNull(item.getItemProperty("address.postalCode"));
	}

	@Test
	public void testPropertyValue_Unbuffered() {
		final Property prop = item.getItemProperty("firstName");
		final boolean[] listenerCalled = new boolean[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0] = true;
					}
				});

		assertTrue(item.isReadThrough());
		assertTrue(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertFalse(listenerCalled[0]);

		prop.setValue("Hello");

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertEquals("Hello", item.getEntity().getFirstName());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertTrue(listenerCalled[0]);
		assertEquals("firstName", modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testPropertyValueFromString_Unbuffered() {
		final Property prop = item.getItemProperty("id");
		assertEquals(123l, prop.getValue());
		prop.setValue("1234");
		assertEquals(1234l, prop.getValue());
	}

	@Test
	public void testNestedPropertyValue_Unbuffered() {
		final Property prop = item.getItemProperty("address.street");
		final boolean[] listenerCalled = new boolean[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0] = true;
					}
				});

		assertTrue(item.isReadThrough());
		assertTrue(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertFalse(listenerCalled[0]);

		prop.setValue("World");

		assertEquals("World", prop.getValue());
		assertEquals("World", prop.toString());
		assertEquals("World", item.getEntity().getAddress().getStreet());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertTrue(listenerCalled[0]);
		assertEquals("address.street", modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testLocalNestedPropertyValue_Unbuffered() {
		item.addNestedContainerProperty("address.postalCode");
		final Property prop = item.getItemProperty("address.postalCode");
		final boolean[] listenerCalled = new boolean[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0] = true;
					}
				});

		assertTrue(item.isReadThrough());
		assertTrue(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertFalse(listenerCalled[0]);

		prop.setValue("World");

		assertEquals("World", prop.getValue());
		assertEquals("World", prop.toString());
		assertEquals("World", item.getEntity().getAddress().getPostalCode());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertTrue(listenerCalled[0]);
		assertEquals("address.postalCode", modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testPropertyValue_Unbuffered_ReadOnly() {
		entity.setFirstName("Joe");
		entity.setLastName("Cool");
		final Property prop = item.getItemProperty("fullName");
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						fail("No listener should be called");
					}
				});
		try {
			prop.setValue("Blah");
			fail("No exception thrown");
		} catch (ReadOnlyException e) {
			assertEquals("Joe Cool", prop.getValue());
			assertFalse(item.isDirty());
			assertFalse(item.isModified());
		}
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testNestedPropertyValue_Unbuffered_ReadOnly() {
		entity.getAddress().setStreet("Street");
		entity.getAddress().setPostalCode("1234");
		entity.getAddress().setPostOffice("Office");
		final Property prop = item.getItemProperty("address.fullAddress");
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						fail("No listener should be called");
					}
				});
		try {
			prop.setValue("Blah");
			fail("No exception thrown");
		} catch (ReadOnlyException e) {
			assertEquals("Street 1234 Office", prop.toString());
			assertFalse(item.isDirty());
			assertFalse(item.isModified());
		}
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testPropertyValue_Buffered_NoReadThrough_Commit() {
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		final Property prop = item.getItemProperty("firstName");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.commit();

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertEquals("Hello", item.getEntity().getFirstName());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertEquals(1, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testNestedPropertyValue_Buffered_NoReadThrough_Commit() {
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		final Property prop = item.getItemProperty("address.street");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getAddress().getStreet());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.commit();

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertEquals("Hello", item.getEntity().getAddress().getStreet());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertEquals(1, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testLocalNestedPropertyValue_Buffered_NoReadThrough_Commit() {
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		item.addNestedContainerProperty("address.postalCode");
		final Property prop = item.getItemProperty("address.postalCode");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getAddress().getPostalCode());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.commit();

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertEquals("Hello", item.getEntity().getAddress().getPostalCode());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertEquals(1, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testPropertyValue_Buffered_NoReadThrough_Discard() {
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		final Property prop = item.getItemProperty("firstName");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.discard();

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(2, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testNestedPropertyValue_Buffered_NoReadThrough_Discard() {
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		final Property prop = item.getItemProperty("address.street");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getAddress().getStreet());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.discard();

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getAddress().getStreet());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(2, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testLocalNestedPropertyValue_Buffered_NoReadThrough_Discard() {
		item.addNestedContainerProperty("address.postalCode");
		item.setWriteThrough(false);
		// item.setReadThrough(false);

		final Property prop = item.getItemProperty("address.postalCode");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertFalse(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is false, so we should get the cached value
		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getAddress().getPostalCode());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(1, listenerCalled[0]);

		item.discard();

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getAddress().getPostalCode());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(2, listenerCalled[0]);
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testPropertyValue_Buffered_ReadThrough_Commit() {
		item.setWriteThrough(false);
		item.setReadThrough(true);

		final Property prop = item.getItemProperty("firstName");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertTrue(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is true, so we should still get the real value
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(0, listenerCalled[0]); // To the observers, the value has
											// not changed!

		// Now, we temporarily turn off read through
		item.setReadThrough(false);

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertEquals(1, listenerCalled[0]); // Now the value has changed to the
											// observers

		// Now, we turn on read through again
		item.setReadThrough(true);

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertEquals(2, listenerCalled[0]); // Now the value has changed back to
											// null

		item.commit();

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertEquals("Hello", item.getEntity().getFirstName());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertEquals(3, listenerCalled[0]); // The value has changed yet another
											// time
		assertNull(modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testPropertyValue_Buffered_ReadThrough_Discard() {
		item.setWriteThrough(false);
		item.setReadThrough(true);

		final Property prop = item.getItemProperty("firstName");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		assertTrue(item.isReadThrough());
		assertFalse(item.isWriteThrough());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertEquals(0, listenerCalled[0]);

		prop.setValue("Hello");

		// Read through is true, so we should still get the real value
		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertTrue(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(0, listenerCalled[0]); // To the observers, the value has
											// not changed!

		// Now, we temporarily turn off read through
		item.setReadThrough(false);

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertEquals(1, listenerCalled[0]); // Now the value has changed to the
											// observers

		// Now, we turn on read through again
		item.setReadThrough(true);

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertEquals(2, listenerCalled[0]); // Now the value has changed back to
											// null

		item.discard();

		assertNull(prop.getValue());
		assertNull(prop.toString());
		assertNull(item.getEntity().getFirstName());
		assertFalse(item.isModified());
		assertFalse(item.isDirty());
		assertEquals(2, listenerCalled[0]); // The value has NOT changed to the
											// observers
		assertNull(modifiedPropertyId);
		assertNull(modifiedItem);
	}

	@Test
	public void testTurnOnWriteThrough() {
		item.setWriteThrough(false);

		final Property prop = item.getItemProperty("address.street");
		final int[] listenerCalled = new int[1];
		((Property.ValueChangeNotifier) prop)
				.addListener(new Property.ValueChangeListener() {

					public void valueChange(ValueChangeEvent event) {
						assertSame(prop, event.getProperty());
						listenerCalled[0]++;
					}
				});

		prop.setValue("Hello");
		assertEquals(1, listenerCalled[0]);

		item.setWriteThrough(true);

		assertEquals("Hello", prop.getValue());
		assertEquals("Hello", item.getEntity().getAddress().getStreet());
		assertFalse(item.isModified());
		assertTrue(item.isDirty());
		assertEquals(2, listenerCalled[0]);
		assertTrue(item.isWriteThrough());
		assertTrue(item.isReadThrough()); // Has also been changed
		assertNull(modifiedPropertyId);
		assertSame(item, modifiedItem);
	}

	@Test
	public void testTurnOffReadThroughWithWriteThroughActive() {
		try {
			item.setReadThrough(false);
		} catch (IllegalStateException e) {
			assertTrue(item.isReadThrough());
		}
	}

	// TODO Test registering property listeners through item
}
