/*******************************************************************************
 * Copyright (c) 2011, 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.widgets;

import static org.eclipse.rap.rwt.testfixture.internal.SerializationTestUtil.serializeAndDeserialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rap.rwt.internal.lifecycle.RemoteAdapter;
import org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil;
import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.widgets.WidgetRemoteAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class WidgetSerialization_Test {

  private Widget widget;

  @Before
  public void setUp() {
    Fixture.setUp();
    widget = new TestWidget();
    widget.display = new Display();
  }

  @After
  public void tearDown() {
    Fixture.tearDown();
  }

  @Test
  public void testRemoteAdapterIsSerializable() throws Exception {
    WidgetRemoteAdapter adapter = getWidgetRemoteAdapter( widget );
    adapter.setInitialized( true );

    Widget deserializedWidget = serializeAndDeserialize( widget );
    RemoteAdapter deserializedAdapter = getWidgetRemoteAdapter( deserializedWidget );

    assertNotNull( deserializedAdapter );
    assertEquals( adapter.getId(), deserializedAdapter.getId() );
    assertTrue( adapter.isInitialized() == deserializedAdapter.isInitialized() );
  }

  @Test
  public void testStyleIsSerializable() throws Exception {
    int style = 1234;
    widget.style = style;

    Widget deserializedWidget = serializeAndDeserialize( widget );

    assertEquals( style, deserializedWidget.style );
  }

  @Test
  public void testStateIsSerializable() throws Exception {
    int state = 1 << 23;
    widget.addState( state );

    Widget deserializedWidget = serializeAndDeserialize( widget );

    assertTrue( deserializedWidget.hasState( state ) );
  }

  @Test
  public void testDataIsSerializable() throws Exception {
    String data = "data";
    String key = "key";
    String keyedData = "keyedData";
    widget.setData( data );
    widget.setData( key, keyedData );

    Widget deserializedWidget = serializeAndDeserialize( widget );

    assertEquals( data, deserializedWidget.getData() );
    assertEquals( keyedData, deserializedWidget.getData( key ) );
  }

  @Test
  public void testListenerIsSerializable() throws Exception {
    widget.addListener( SWT.Dispose, new TestListener() );

    Widget deserializedWidget = serializeAndDeserialize( widget );

    Listener[] listeners = deserializedWidget.getListeners( SWT.Dispose );
    assertEquals( 1, listeners.length );
  }

  @Test
  public void testPreservedValuesAreNotSerialized() throws Exception {
    String propertyName = "foo";
    WidgetRemoteAdapter adapter = getWidgetRemoteAdapter( widget );
    adapter.preserve( propertyName, "bar" );

    Widget deserializedWidget = serializeAndDeserialize( widget );
    RemoteAdapter deserializedAdapter = getWidgetRemoteAdapter( deserializedWidget );

    assertNull( deserializedAdapter.getPreserved( propertyName ) );
  }

  private static WidgetRemoteAdapter getWidgetRemoteAdapter( Widget widget ) {
    return ( WidgetRemoteAdapter )WidgetUtil.getAdapter( widget );
  }

  private static class TestListener implements Listener {
    @Override
    public void handleEvent( Event event ) {
    }
  }

  private static class TestWidget extends Widget {
  }

}
