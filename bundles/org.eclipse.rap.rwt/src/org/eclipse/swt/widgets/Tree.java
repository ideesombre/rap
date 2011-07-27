/*******************************************************************************
 * Copyright (c) 2002, 2011 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.widgets;

import java.util.*;
import java.util.List;

import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.rwt.internal.theme.IThemeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.SerializableCompatibility;
import org.eclipse.swt.internal.events.SetDataEvent;
import org.eclipse.swt.internal.widgets.*;
import org.eclipse.swt.internal.widgets.WidgetTreeVisitor.AllWidgetTreeVisitor;
import org.eclipse.swt.internal.widgets.treekit.TreeThemeAdapter;


/**
 * Instances of this class provide a selectable user interface object that
 * displays a hierarchy of items and issues notification when an item in the
 * hierarchy is selected.
 * <p>
 * The item children that may be added to instances of this class must be of
 * type <code>TreeItem</code>.
 * </p>
 * <p>
 * Style <code>VIRTUAL</code> is used to create a <code>Tree</code> whose
 * <code>TreeItem</code>s are to be populated by the client on an on-demand
 * basis instead of up-front. This can provide significant performance
 * improvements for trees that are very large or for which <code>TreeItem</code>
 * population is expensive (for example, retrieving values from an external
 * source).
 * </p>
 * <p>
 * Here is an example of using a <code>Tree</code> with style
 * <code>VIRTUAL</code>: <code><pre>
 *  final Tree tree = new Tree(parent, SWT.VIRTUAL | SWT.BORDER);
 *  tree.setItemCount(20);
 *  tree.addListener(SWT.SetData, new Listener() {
 *      public void handleEvent(Event event) {
 *          TreeItem item = (TreeItem)event.item;
 *          TreeItem parentItem = item.getParentItem();
 *          String text = null;
 *          if (parentItem == null) {
 *              text = "node " + tree.indexOf(item);
 *          } else {
 *              text = parentItem.getText() + " - " + parentItem.indexOf(item);
 *          }
 *          item.setText(text);
 *          System.out.println(text);
 *          item.setItemCount(10);
 *      }
 *  });
 * </pre></code>
 * </p>
 * <p>
 * Note that although this class is a subclass of <code>Composite</code>, it
 * does not make sense to add <code>Control</code> children to it, or set a
 * layout on it.
 * </p>
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SINGLE, MULTI, CHECK, FULL_SELECTION, VIRTUAL, NO_SCROLL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection, Collapse, Expand, SetData<!--, MeasureItem,
 * EraseItem, PaintItem--></dd>
 * </dl>
 * </p>
 * <p>
 * Note: Only one of the styles SINGLE and MULTI may be specified.
 * </p>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @since 1.0
 */
public class Tree extends Composite {

  private static final TreeItem[] EMPTY_SELECTION = new TreeItem[ 0 ];
  // This values must be kept in sync with appearance of list items
  private static final int MIN_ITEM_HEIGHT = 16;
  private static final int GRID_WIDTH = 1;

  private static final Rectangle TEXT_MARGIN = new Rectangle( 3, 0, 8, 0 );

  private int itemCount;
  private TreeItem[] items;
  final ItemHolder<TreeColumn> columnHolder;
  private TreeItem[] selection;
  private boolean linesVisible;
  private int[] columnOrder;
  private int itemImageCount = 0;
  private TreeColumn sortColumn;
  private int sortDirection = SWT.NONE;
  private boolean headerVisible = false;
  private final ResizeListener resizeListener;
  private final ITreeAdapter treeAdapter;
  private int scrollLeft = 0;
  private int topItemIndex = 0;
  private boolean hasVScrollBar;
  private boolean hasHScrollBar;
  private ScrollBar verticalBar;
  private ScrollBar horizontalBar;
  private Point itemImageSize;
  private Rectangle bufferedCellPadding = null;
  private int bufferedCellSpacing = -1;
  private Rectangle bufferedCheckBoxMargin = null;

  private final class CompositeItemHolder implements IItemHolderAdapter {
    public void add( Item item ) {
      if( item instanceof TreeColumn ) {
        columnHolder.add( ( TreeColumn )item );
      } else {
        String msg = "Only TreeColumns may be added to CompositeItemHolder";
        throw new IllegalArgumentException( msg );
      }
    }
    public void insert( Item item, int index ) {
      if( item instanceof TreeColumn ) {
        columnHolder.insert( ( TreeColumn )item, index );
      } else {
        String msg = "Only TreeColumns may be inserted to CompositeItemHolder";
        throw new IllegalArgumentException( msg );
      }
    }
    public void remove( Item item ) {
      if( item instanceof TreeColumn ) {
        columnHolder.remove( ( TreeColumn )item );
      } else {
        String msg = "Only TreeColumns may be removed from CompositeItemHolder";
        throw new IllegalArgumentException( msg );
      }
    }
    public Item[] getItems() {
      TreeItem[] items = getCreatedItems();
      Item[] columns = columnHolder.getItems();
      Item[] result = new Item[ columns.length + items.length ];
      System.arraycopy( columns, 0, result, 0, columns.length );
      System.arraycopy( items, 0, result, columns.length, items.length );
      return result;
    }
  }
  
  private final class InternalTreeAdapter 
    implements ITreeAdapter, ICellToolTipAdapter, SerializableCompatibility
  {
    private String toolTipText;
    private ICellToolTipProvider provider;

    public void setScrollLeft( int left ) {
      Tree.this.scrollLeft = left;
    }

    public int getScrollLeft() {
      return Tree.this.scrollLeft;
    }

    public boolean isCached( TreeItem item ) {
      return item.isCached();
    }

    public void checkAllData( Tree tree ) {
      tree.checkAllData();
    }

    public boolean hasHScrollBar() {
      return Tree.this.hasHScrollBar();
    }

    public boolean hasVScrollBar() {
      return Tree.this.hasVScrollBar();
    }

    public Point getItemImageSize( int index ) {
      return Tree.this.getItemImageSize( index );
    }

    public int getCellLeft( int index ) {
      return Tree.this.getCellLeft( index );
    }

    public int getCellWidth( int index ) {
      return Tree.this.getCellWidth( index );
    }

    public int getTextOffset( int index ) {
      return Tree.this.getTextOffset( index );
    }

    public int getTextMaxWidth( int index ) {
      return Tree.this.getTextWidth( index );
    }

    public int getCheckWidth() {
      return Tree.this.getCheckImageSize().x;
    }

    public int getImageOffset( int index ) {
      return Tree.this.getImageOffset( index );
    }

    public int getIndentionWidth() {
      return Tree.this.getIndentionWidth();
    }

    public int getCheckLeft() {
      return getCheckBoxMargin().x;
    }

    public Rectangle getTextMargin() {
      return TEXT_MARGIN;
    }

    public int getTopItemIndex() {
      return Tree.this.getTopIndex();
    }

    public void setTopItemIndex( int index ) {
      Tree.this.setTopItemIndex( index );
    }

    public int getColumnLeft( TreeColumn column ) {
      int index = Tree.this.indexOf( column );
      return Tree.this.getColumn( index ).getLeft();
    }

    public ICellToolTipProvider getCellToolTipProvider() {
      return provider;
    }

    public void setCellToolTipProvider( ICellToolTipProvider provider ) {
      this.provider = provider;
    }

    public String getCellToolTipText() {
      return toolTipText;
    }

    public void setCellToolTipText( String toolTipText ) {
      this.toolTipText = toolTipText;
    }

  }

  private static final class ResizeListener extends ControlAdapter {
    public void controlResized( ControlEvent event ) {
      Tree tree = ( Tree )event.widget;
      if( tree.isVirtual() ) {
        tree.checkAllData();
      }
      tree.updateScrollBars();
    }
  }

  /**
   * Constructs a new instance of this class given its parent and a style value
   * describing its behavior and appearance.
   * <p>
   * The style value is either one of the style constants defined in class
   * <code>SWT</code> which is applicable to instances of this class, or must be
   * built by <em>bitwise OR</em>'ing together (that is, using the
   * <code>int</code> "|" operator) two or more of those <code>SWT</code> style
   * constants. The class description lists the style constants that are
   * applicable to the class. Style bits are also inherited from superclasses.
   * </p>
   *
   * @param parent a composite control which will be the parent of the new
   *          instance (cannot be null)
   * @param style the style of control to construct
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the parent</li>
   *              <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed
   *              subclass</li>
   *              </ul>
   * @see SWT#SINGLE
   * @see SWT#MULTI
   * @see SWT#CHECK
   * @see SWT#FULL_SELECTION
   * @see SWT#NO_SCROLL
   * @see Widget#checkSubclass
   * @see Widget#getStyle
   */
  public Tree( Composite parent, int style ) {
    super( parent, checkStyle( style ) );
    columnHolder = new ItemHolder<TreeColumn>( TreeColumn.class );
    treeAdapter = new InternalTreeAdapter();
    setTreeEmpty();
    createScrollBars();
    selection = EMPTY_SELECTION;
    resizeListener = new ResizeListener();
    addControlListener( resizeListener );
  }

  private TreeItem[] getCreatedItems() {
    TreeItem[] result;
    if( isVirtual() ) {
      int count = 0;
      for( int i = 0; i < itemCount; i++ ) {
        if( items[ i ] != null ) {
          count++;
        }
      }
      result = new TreeItem[ count ];
      count = 0;
      for( int i = 0; i < itemCount; i++ ) {
        if( items[ i ] != null ) {
          result[ count ] = items[ i ];
          count++;
        }
      }
    } else {
      result = new TreeItem[ itemCount ];
      System.arraycopy( items, 0, result, 0, itemCount );
    }
    return result;
  }
  
  private void setTreeEmpty() {
    items = new TreeItem[ 4 ];
    // TODO: Not sure if we have to clear the image size???!!!
//    clearItemImageSize();
  }

  void initState() {
    state &= ~( /* CANVAS | */THEME_BACKGROUND );
  }

  public Object getAdapter( Class adapter ) {
    Object result;
    if( adapter == IItemHolderAdapter.class ) {
      result = new CompositeItemHolder();
    } else if( adapter == ITreeAdapter.class ) {
      result = treeAdapter;
    } else if( adapter == ICellToolTipAdapter.class ) {
      result = treeAdapter;
    } else {
      result = super.getAdapter( adapter );
    }
    return result;
  }

  public void setFont( Font font ) {
    super.setFont( font );
    TreeItem[] items = getItems();
    for( int i = 0; i < items.length; i++ ) {
      items[ i ].clearPreferredWidthBuffers();
    }
    updateScrollBars();
  }

  // /////////////////////////
  // Methods to manage items
  /**
   * Sets the number of root-level items contained in the receiver.
   *
   * @param count the number of items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setItemCount( int count ) {
    checkWidget();
    int oldItemCount = itemCount;
    int newItemCount = Math.max( 0, count );
    if( newItemCount != oldItemCount ) {
      int deleteIndex = oldItemCount - 1;
      while( deleteIndex >= newItemCount ) {
        TreeItem item = items[ deleteIndex ];
        if( item != null && !item.isDisposed() ) {
          item.dispose();
        } else {
          destroyItem( null, deleteIndex );
        }
        deleteIndex--;
      }
      int length = Math.max( 4, ( newItemCount + 3 ) / 4 * 4 );
      TreeItem[] newItems = new TreeItem[ length ];
      System.arraycopy( items, 0, newItems, 0, Math.min( newItemCount, itemCount ) );
      items = newItems;
      if( !isVirtual() ) {
        for( int i = itemCount; i < newItemCount; i++ ) {
          items[ i ] = new TreeItem( this, SWT.NONE, i );
        }
      }
      itemCount = newItemCount;
      updateScrollBars();
    }
    redraw();
  }

  /**
   * Returns the number of items contained in the receiver that are direct item
   * children of the receiver. The number that is returned is the number of
   * roots in the tree.
   *
   * @return the number of items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int getItemCount() {
    checkWidget();
    return itemCount;
  }

  /**
   * Returns a (possibly empty) array of items contained in the receiver that
   * are direct item children of the receiver. These are the roots of the tree.
   * <p>
   * Note: This is not the actual structure used by the receiver to maintain its
   * list of items, so modifying the array will not affect the receiver.
   * </p>
   *
   * @return the items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public TreeItem[] getItems() {
    checkWidget();
    TreeItem[] result = new TreeItem[ itemCount ];
    if( isVirtual() ) {
      for( int i = 0; i < itemCount; i++ ) {
        result[ i ] = _getItem( i );
      }
    } else {
      System.arraycopy( items, 0, result, 0, itemCount );
    }
    return result;
  }

  private TreeItem _getItem( int index ) {
    if( isVirtual() && items[ index ] == null ) {
      items[ index ] = new TreeItem( this, null, SWT.NONE, index, false );
    }
    return items[ index ];
  }

  /**
   * Returns the item at the given, zero-relative index in the receiver. Throws
   * an exception if the index is out of range.
   *
   * @param index the index of the item to return
   * @return the item at the given index
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_RANGE - if the index is not between 0 and
   *              the number of elements in the list minus 1 (inclusive)</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public TreeItem getItem( int index ) {
    checkWidget();
    if( index < 0 || index >= itemCount ) {
      SWT.error( SWT.ERROR_INVALID_RANGE );
    }
    return _getItem( index );
  }

  /**
   * Searches the receiver's list starting at the first item (index 0) until an
   * item is found that is equal to the argument, and returns the index of that
   * item. If no item is found, returns -1.
   *
   * @param item the search item
   * @return the index of the item
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the tool item is null</li>
   *              <li>ERROR_INVALID_ARGUMENT - if the tool item has been
   *              disposed</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int indexOf( TreeItem item ) {
    checkWidget();
    if( item == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      SWT.error( SWT.ERROR_INVALID_ARGUMENT );
    }
    return item.parent == this ? item.index : -1;
  }

  /**
   * Returns the receiver's parent item, which must be a <code>TreeItem</code>
   * or null when the receiver is a root.
   *
   * @return the receiver's parent item
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public TreeItem getParentItem() {
    checkWidget();
    return null;
  }

  /**
   * Removes all of the items from the receiver.
   *
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void removeAll() {
    checkWidget();
    TreeItem[] items = getItems();
    for( int i = 0; i < items.length; i++ ) {
      items[ i ].dispose();
    }
    selection = EMPTY_SELECTION;
  }

  /**
   * Shows the item. If the item is already showing in the receiver, this method
   * simply returns. Otherwise, the items are scrolled and expanded until the
   * item is visible.
   *
   * @param item the item to be shown
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the item is null</li>
   *              <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#showSelection()
   */
  public void showItem( TreeItem item ) {
    checkWidget();
    if( item == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( item.getParent() != this ) {
      return;
    }
    TreeItem parent = item.getParentItem();
    while( parent != null ) {
      parent.setExpanded( true );
      parent = parent.getParentItem();
    }
    if( item.flatIndex <= topItemIndex ) {
      setTopItemIndex( item.flatIndex );
    } else {
      int itemsAreaHeight = getClientArea().height - getHeaderHeight();
      int rows = ( int )Math.floor( itemsAreaHeight / getItemHeight() );
      if( item.flatIndex >= topItemIndex + rows ) {
        setTopItemIndex( item.flatIndex - rows + 1 );
      }
    }
  }

  /**
   * Sets the item which is currently at the top of the receiver.
   * This item can change when items are expanded, collapsed, scrolled
   * or new items are added or removed.
   *
   * @param item the item to be shown
   *
   * @exception IllegalArgumentException <ul>
   *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
   *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
   * </ul>
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @see Tree#getTopItem()
   *
   * @since 1.4
   */
  public void setTopItem( TreeItem item ) {
    checkWidget();
    if( item == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( item.getParent() == this ) {
      TreeItem parent = item.getParentItem();
      while( parent != null ) {
        parent.setExpanded( true );
        parent = parent.getParentItem();
      }
      int visibleItemsCount = collectVisibleItems( null ).size();
      int itemsAreaHeight = getClientArea().height - getHeaderHeight();
      int rows = ( int )Math.floor( itemsAreaHeight / getItemHeight() );
      if(    item.flatIndex <= topItemIndex
          || item.flatIndex + rows <= visibleItemsCount )
      {
        setTopItemIndex( item.flatIndex );
      } else {
        int index = Math.max( 0, visibleItemsCount - rows );
        setTopItemIndex( index );
      }
    }
  }

  /**
   * Returns the item which is currently at the top of the receiver.
   * This item can change when items are expanded, collapsed, scrolled
   * or new items are added or removed.
   *
   * @return the item at the top of the receiver
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.4
   */
  public TreeItem getTopItem() {
    checkWidget();
    TreeItem result = null;
    if( getItemCount() > 0 ) {
      List visibleItems = collectVisibleItems( null );
      result = ( TreeItem )visibleItems.get( topItemIndex );
    }
    return result;
  }

  private void setTopItemIndex( int index ) {
    if( index != topItemIndex ) {
      topItemIndex = index;
      checkAllData();
    }
  }

  int getTopIndex() {
    return topItemIndex;
  }

  /**
   * Shows the column.  If the column is already showing in the receiver,
   * this method simply returns.  Otherwise, the columns are scrolled until
   * the column is visible.
   *
   * @param column the column to be shown
   *
   * @exception IllegalArgumentException <ul>
   *    <li>ERROR_NULL_ARGUMENT - if the column is null</li>
   *    <li>ERROR_INVALID_ARGUMENT - if the column has been disposed</li>
   * </ul>
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.3
   */
  public void showColumn( TreeColumn column ) {
    checkWidget();
    if( column == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( column.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( column.getParent() == this ) {
      int index = indexOf( column );
      if( 0 <= index && index < getColumnCount() ) {
        int leftColumnsWidth = 0;
        int columnWidth = column.getWidth();
        int clientWidth = getClientArea().width;
        int[] columnOrder = getColumnOrder();
        boolean found = false;
        for( int i = 0; i < columnOrder.length && !found; i++ ) {
          found = index == columnOrder[ i ];
          if( !found ) {
            leftColumnsWidth += getColumn( columnOrder[ i ] ).getWidth();
          }
        }
        if( scrollLeft > leftColumnsWidth ) {
          scrollLeft = leftColumnsWidth;
        } else if( scrollLeft < leftColumnsWidth + columnWidth - clientWidth ) {
          scrollLeft = leftColumnsWidth + columnWidth - clientWidth;
        }
      }
    }
  }

  /**
   * Shows the selection. If the selection is already showing in the receiver,
   * this method simply returns. Otherwise, the items are scrolled until the
   * selection is visible.
   *
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#showItem(TreeItem)
   */
  public void showSelection() {
    checkWidget();
    if( selection.length == 0 ) {
      return;
    }
    showItem( selection[ 0 ] );
  }

  // ///////////////////////////////////
  // Methods to get/set/clear selection
  /**
   * Returns an array of <code>TreeItem</code>s that are currently selected in
   * the receiver. The order of the items is unspecified. An empty array
   * indicates that no items are selected.
   * <p>
   * Note: This is not the actual structure used by the receiver to maintain its
   * selection, so modifying the array will not affect the receiver.
   * </p>
   *
   * @return an array representing the selection
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public TreeItem[] getSelection() {
    checkWidget();
    TreeItem[] result = new TreeItem[ selection.length ];
    System.arraycopy( selection, 0, result, 0, selection.length );
    return result;
  }

  /**
   * Returns the number of selected items contained in the receiver.
   *
   * @return the number of selected items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int getSelectionCount() {
    checkWidget();
    return selection.length;
  }

  /**
   * Sets the receiver's selection to the given item. The current selection is
   * cleared before the new item is selected.
   * <p>
   * If the item is not in the receiver, then it is ignored.
   * </p>
   *
   * @param selection the item to select
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the item is null</li>
   *              <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setSelection( TreeItem selection ) {
    checkWidget();
    if( selection == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    setSelection( new TreeItem[]{
      selection
    } );
  }

  /**
   * Sets the receiver's selection to be the given array of items. The current
   * selection is cleared before the new items are selected.
   * <p>
   * Items that are not in the receiver are ignored. If the receiver is
   * single-select and multiple items are specified, then all items are ignored.
   * </p>
   *
   * @param selection the array of items
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the array of items is null</li>
   *              <li>ERROR_INVALID_ARGUMENT - if one of the items has been
   *              disposed</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#deselectAll()
   */
  public void setSelection( TreeItem[] selection ) {
    checkWidget();
    if( selection == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    int length = selection.length;
    if( ( style & SWT.SINGLE ) != 0 ) {
      if( length == 0 || length > 1 ) {
        deselectAll();
      } else {
        TreeItem item = selection[ 0 ];
        if( item != null ) {
          if( item.isDisposed() ) {
            SWT.error( SWT.ERROR_INVALID_ARGUMENT );
          }
          this.selection = new TreeItem[]{
            item
          };
        }
      }
    } else {
      if( length == 0 ) {
        deselectAll();
      } else {
        // Construct an array that contains all non-null items to be selected
        TreeItem[] validSelection = new TreeItem[ length ];
        int validLength = 0;
        for( int i = 0; i < length; i++ ) {
          if( selection[ i ] != null ) {
            if( selection[ i ].isDisposed() ) {
              SWT.error( SWT.ERROR_INVALID_ARGUMENT );
            }
            validSelection[ validLength ] = selection[ i ];
            validLength++;
          }
        }
        if( validLength > 0 ) {
          // Copy the above created array to its 'final destination'
          this.selection = new TreeItem[ validLength ];
          System.arraycopy( validSelection, 0, this.selection, 0, validLength );
        }
      }
    }
  }

  /**
   * Selects an item in the receiver.  If the item was already
   * selected, it remains selected.
   *
   * @param item the item to be selected
   *
   * @exception IllegalArgumentException <ul>
   *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
   *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
   * </ul>
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.3
   */
  public void select( TreeItem item ) {
    checkWidget();
    if( item == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( ( style & SWT.SINGLE ) != 0 ) {
      setSelection( item );
    } else {
      List<TreeItem> selItems = new ArrayList<TreeItem>( Arrays.asList( selection ) );
      if( !selItems.contains( item ) ) {
        selItems.add( item );
        selection = new TreeItem[ selItems.size() ];
        selItems.toArray( selection );
      }
    }
  }

  /**
   * Selects all of the items in the receiver.
   * <p>
   * If the receiver is single-select, do nothing.
   * </p>
   *
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void selectAll() {
    checkWidget();
    if( ( style & SWT.MULTI ) != 0 ) {
      final java.util.List<TreeItem> allItems = new ArrayList<TreeItem>();
      WidgetTreeVisitor.accept( this, new AllWidgetTreeVisitor() {

        public boolean doVisit( Widget widget ) {
          if( widget instanceof TreeItem ) {
            allItems.add( ( TreeItem )widget );
          }
          return true;
        }
      } );
      selection = new TreeItem[ allItems.size() ];
      allItems.toArray( selection );
    }
  }

  /**
   * Deselects an item in the receiver.  If the item was already
   * deselected, it remains deselected.
   *
   * @param item the item to be deselected
   *
   * @exception IllegalArgumentException <ul>
   *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
   *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
   * </ul>
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.3
   */
  public void deselect( TreeItem item ) {
    checkWidget();
    if( item == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    List<TreeItem> selItems = new ArrayList<TreeItem>( Arrays.asList( selection ) );
    if( selItems.contains( item ) ) {
      selItems.remove( item );
      selection = new TreeItem[ selItems.size() ];
      selItems.toArray( selection );
    }
  }

  /**
   * Deselects all selected items in the receiver.
   *
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void deselectAll() {
    checkWidget();
    this.selection = EMPTY_SELECTION;
  }

  /**
   * Marks the receiver's lines as visible if the argument is <code>true</code>,
   * and marks it invisible otherwise.
   * <p>
   * If one of the receiver's ancestors is not visible or some other condition
   * makes the receiver not visible, marking it visible may not actually cause
   * it to be displayed.
   * </p>
   *
   * @param value the new visibility state
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setLinesVisible( boolean value ) {
    checkWidget();
    if( linesVisible == value ) {
      return; /* no change */
    }
    linesVisible = value;
  }

  /**
   * Returns the width in pixels of a grid line.
   *
   * @return the width of a grid line in pixels
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.4
   */
  public int getGridLineWidth() {
    checkWidget();
    return GRID_WIDTH;
  }

  /**
   * Returns <code>true</code> if the receiver's lines are visible, and
   * <code>false</code> otherwise.
   * <p>
   * If one of the receiver's ancestors is not visible or some other condition
   * makes the receiver not visible, this method may still indicate that it is
   * considered visible even though it may not actually be showing.
   * </p>
   *
   * @return the visibility state of the lines
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public boolean getLinesVisible() {
    checkWidget();
    return linesVisible;
  }

  /**
   * Clears the item at the given zero-relative index in the receiver. The text,
   * icon and other attributes of the item are set to the default value. If the
   * tree was created with the <code>SWT.VIRTUAL</code> style, these attributes
   * are requested again as needed.
   *
   * @param index the index of the item to clear
   * @param recursive <code>true</code> if all child items of the indexed item
   *          should be cleared recursively, and <code>false</code> otherwise
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_RANGE - if the index is not between 0 and
   *              the number of elements in the list minus 1 (inclusive)</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SWT#VIRTUAL
   * @see SWT#SetData
   */
  public void clear( int index, boolean recursive ) {
    checkWidget();
    if( index < 0 || index >= itemCount ) {
      error( SWT.ERROR_INVALID_RANGE );
    }
    TreeItem item = items[ index ];
    if( item != null ) {
      item.clear();
      if( recursive ) {
        item.clearAll( true, false );
      }
      checkData( item, index );
    }
  }

  /**
   * Returns the item at the given point in the receiver or null if no such item
   * exists. The point is in the coordinate system of the receiver.
   * <p>
   * The item that is returned represents an item that could be selected by the
   * user. For example, if selection only occurs in items in the first column,
   * then null is returned if the point is outside of the item. Note that the
   * SWT.FULL_SELECTION style hint, which specifies the selection policy,
   * determines the extent of the selection.
   * </p>
   *
   * @param point the point used to locate the item
   * @return the item at the given point, or null if the point is not in a
   *         selectable item
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the point is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public TreeItem getItem( Point point ) {
    checkWidget();
    if( point == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    TreeItem result = null;
    int index = ( point.y - getHeaderHeight() ) / getItemHeight() + topItemIndex;
    // collect all visible items
    List visibleItems = collectVisibleItems( null );
    if( 0 <= index && index < visibleItems.size() ) {
      result = ( TreeItem )visibleItems.get( index );
    }
    return result;
  }

  private List<TreeItem> collectVisibleItems( TreeItem parent ) {
    List<TreeItem> result = new ArrayList<TreeItem>();
    TreeItem[] children;
    if( parent == null ) {
      children = getItems();
    } else {
      children = parent.getItems();
    }
    for( int i = 0; i < children.length; i++ ) {
      TreeItem item = children[ i ];
      result.add( item );
      if( item.getExpanded() ) {
        result.addAll( collectVisibleItems( item ) );
      }
    }
    return result;
  }

  /**
   * Returns the height of the area which would be used to display <em>one</em>
   * of the items in the tree.
   *
   * @return the height of one item
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   *
   * @since 1.3
   */
  public int getItemHeight() {
    checkWidget();
    Rectangle padding = getCellPadding();
    int textHeight = Graphics.getCharHeight( getFont() );
    textHeight += TEXT_MARGIN.height + padding.height;
    int itemImageHeight = getItemImageSize().y + padding.height;
    int result = Math.max( itemImageHeight, textHeight );
    if( hasCheckBoxes( 0 )) {
      result = Math.max( getCheckImageOuterSize().y, result );
    }
    result += 1; // The space needed for horizontal gridline is always added, even if not visible
    result = Math.max( result, MIN_ITEM_HEIGHT );
    return result;
  }

  /**
   * Clears all the items in the receiver. The text, icon and other attributes
   * of the items are set to their default values. If the tree was created with
   * the <code>SWT.VIRTUAL</code> style, these attributes are requested again as
   * needed.
   *
   * @param recursive <code>true</code> if all child items should be cleared
   *          recursively, and <code>false</code> otherwise
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SWT#VIRTUAL
   * @see SWT#SetData
   */
  public void clearAll( boolean recursive ) {
    checkWidget();
    for( int i = 0; i < itemCount; i++ ) {
      TreeItem item = items[ i ];
      if( item != null ) {
        item.clear();
        if( recursive ) {
          item.clearAll( true, false );
        }
      }
    }
    if( itemCount != 0 ) {
      if( isVirtual() ) {
        checkAllData();
      }
    }
  }
  
  public void changed( Control[] changed ) {
    clearItemsPreferredWidthBuffer();
    super.changed( changed );
  }

  private void clearItemsPreferredWidthBuffer() {
    for( int i = 0; i < itemCount; i++ ) {
      TreeItem item = items[ i ];
      if( item != null ) {
        item.clearPreferredWidthBuffers();
      }
    }
  }

  /**
   * Returns the number of columns contained in the receiver. If no
   * <code>TreeColumn</code>s were created by the programmer, this value is
   * zero, despite the fact that visually, one column of items may be visible.
   * This occurs when the programmer uses the tree like a list, adding items but
   * never creating a column.
   *
   * @return the number of columns
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int getColumnCount() {
    checkWidget();
    return columnHolder.size();
  }

  void createColumn( TreeColumn column, int index ) {
    columnHolder.insert( column, index );
    if( columnOrder == null ) {
      columnOrder = new int[]{
        index
      };
    } else {
      int length = columnOrder.length;
      for( int i = index; i < length; i++ ) {
        columnOrder[ i ]++;
      }
      int[] newColumnOrder = new int[ length + 1 ];
      System.arraycopy( columnOrder, 0, newColumnOrder, 0, index );
      System.arraycopy( columnOrder, index, newColumnOrder, index + 1, length - index );
      columnOrder = newColumnOrder;
      columnOrder[ index ] = index;
    }
    for( int i = 0; i < itemCount; i++ ) {
      if( items[ i ] != null ) {
        items[ i ].shiftData( index );
      }
    }
    updateScrollBars();
  }

  final void destroyColumn( TreeColumn column ) {
    if( !isInDispose() ) {
      int index = indexOf( column );
      // Remove data from TreeItems
      for( int i = 0; i < itemCount; i++ ) {
        if( items[ i ] != null ) {
          items[ i ].removeData( index );
        }
      }
      // Reset sort column if necessary
      if( column == sortColumn ) {
        sortColumn = null;
      }
      // Remove from column holder
      columnHolder.remove( column );
      // Remove from column order
      int length = columnOrder.length;
      int[] newColumnOrder = new int[ length - 1 ];
      int count = 0;
      for( int i = 0; i < length; i++ ) {
        if( columnOrder[ i ] != index ) {
          int newOrder = columnOrder[ i ];
          if( index < newOrder ) {
            newOrder--;
          }
          newColumnOrder[ count ] = newOrder;
          count++;
        }
      }
      columnOrder = newColumnOrder;
      updateScrollBars();
    }
  }

  /**
   * Returns the height of the receiver's header
   *
   * @return the height of the header or zero if the header is not visible
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int getHeaderHeight() {
    checkWidget();
    int result = 0;
    if( headerVisible ) {
      TreeThemeAdapter themeAdapter = getThemeAdapter();
      Font headerFont = themeAdapter.getHeaderFont( this );
      int textHeight = Graphics.getCharHeight( headerFont );
      int imageHeight = 0;
      for( int i = 0; i < getColumnCount(); i++ ) {
        Image image = getColumn( i ).getImage();
        int height = image == null ? 0 : image.getBounds().height;
        if( height > imageHeight ) {
          imageHeight = height;
        }
      }
      result = Math.max( textHeight, imageHeight );
      result += themeAdapter.getHeaderBorderBottomWidth( this );
      result += themeAdapter.getHeaderPadding( this ).height;
    }
    return result;
  }

  /**
   * Marks the receiver's header as visible if the argument is <code>true</code>
   * , and marks it invisible otherwise.
   * <p>
   * If one of the receiver's ancestors is not visible or some other condition
   * makes the receiver not visible, marking it visible may not actually cause
   * it to be displayed.
   * </p>
   *
   * @param value the new visibility state
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setHeaderVisible( boolean value ) {
    checkWidget();
    if( headerVisible == value ) {
      return; /* no change */
    }
    headerVisible = value;
    updateScrollBars();
  }

  /**
   * Returns <code>true</code> if the receiver's header is visible, and
   * <code>false</code> otherwise.
   * <p>
   * If one of the receiver's ancestors is not visible or some other condition
   * makes the receiver not visible, this method may still indicate that it is
   * considered visible even though it may not actually be showing.
   * </p>
   *
   * @return the receiver's header's visibility state
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public boolean getHeaderVisible() {
    checkWidget();
    return headerVisible;
  }

  /**
   * Searches the receiver's list starting at the first column (index 0) until a
   * column is found that is equal to the argument, and returns the index of
   * that column. If no column is found, returns -1.
   *
   * @param column the search column
   * @return the index of the column
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the column is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int indexOf( TreeColumn column ) {
    checkWidget();
    if( column == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    if( column.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    return columnHolder.indexOf( column );
  }

  /**
   * Returns the column at the given, zero-relative index in the receiver.
   * Throws an exception if the index is out of range. Columns are returned in
   * the order that they were created. If no <code>TreeColumn</code>s were
   * created by the programmer, this method will throw
   * <code>ERROR_INVALID_RANGE</code> despite the fact that a single column of
   * data may be visible in the tree. This occurs when the programmer uses the
   * tree like a list, adding items but never creating a column.
   *
   * @param index the index of the column to return
   * @return the column at the given index
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_RANGE - if the index is not between 0 and
   *              the number of elements in the list minus 1 (inclusive)</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#getColumnOrder()
   * @see Tree#setColumnOrder(int[])
   * @see TreeColumn#getMoveable()
   * @see TreeColumn#setMoveable(boolean)
   * @see SWT#Move
   */
  public TreeColumn getColumn( int index ) {
    checkWidget();
    if( !( 0 <= index && index < columnHolder.size() ) ) {
      error( SWT.ERROR_INVALID_RANGE );
    }
    return columnHolder.getItem( index );
  }

  /**
   * Returns an array of <code>TreeColumn</code>s which are the columns in the
   * receiver. Columns are returned in the order that they were created. If no
   * <code>TreeColumn</code>s were created by the programmer, the array is
   * empty, despite the fact that visually, one column of items may be visible.
   * This occurs when the programmer uses the tree like a list, adding items but
   * never creating a column.
   * <p>
   * Note: This is not the actual structure used by the receiver to maintain its
   * list of items, so modifying the array will not affect the receiver.
   * </p>
   *
   * @return the items in the receiver
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#getColumnOrder()
   * @see Tree#setColumnOrder(int[])
   * @see TreeColumn#getMoveable()
   * @see TreeColumn#setMoveable(boolean)
   * @see SWT#Move
   */
  public TreeColumn[] getColumns() {
    checkWidget();
    return columnHolder.getItems();
  }

  /**
   * Sets the order that the items in the receiver should be displayed in to the
   * given argument which is described in terms of the zero-relative ordering of
   * when the items were added.
   *
   * @param order the new order to display the items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the item order is null</li>
   *              <li>ERROR_INVALID_ARGUMENT - if the item order is not the same
   *              length as the number of items</li>
   *              </ul>
   * @see Tree#getColumnOrder()
   * @see TreeColumn#getMoveable()
   * @see TreeColumn#setMoveable(boolean)
   * @see SWT#Move
   */
  public void setColumnOrder( int[] order ) {
    checkWidget();
    if( order == null ) {
      error( SWT.ERROR_NULL_ARGUMENT );
    }
    int columnCount = getColumnCount();
    if( order.length != columnCount ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( columnCount > 0 ) {
      int[] oldOrder = new int[ columnCount ];
      System.arraycopy( columnOrder, 0, oldOrder, 0, columnOrder.length );
      boolean reorder = false;
      boolean[] seen = new boolean[ columnCount ];
      for( int i = 0; i < order.length; i++ ) {
        int index = order[ i ];
        if( index < 0 || index >= columnCount ) {
          error( SWT.ERROR_INVALID_RANGE );
        }
        if( seen[ index ] ) {
          error( SWT.ERROR_INVALID_ARGUMENT );
        }
        seen[ index ] = true;
        if( index != oldOrder[ i ] ) {
          reorder = true;
        }
      }
      if( reorder ) {
        System.arraycopy( order, 0, columnOrder, 0, columnOrder.length );
        for( int i = 0; i < seen.length; i++ ) {
          if( oldOrder[ i ] != columnOrder[ i ] ) {
            TreeColumn column = getColumn( columnOrder[ i ] );
            int controlMoved = ControlEvent.CONTROL_MOVED;
            ControlEvent controlEvent = new ControlEvent( column, controlMoved );
            controlEvent.processEvent();
          }
        }
      }
    }
  }

  /**
   * Sets the column used by the sort indicator for the receiver. A null value
   * will clear the sort indicator. The current sort column is cleared before
   * the new column is set.
   *
   * @param column the column used by the sort indicator or <code>null</code>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_ARGUMENT - if the column is disposed</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setSortColumn( TreeColumn column ) {
    checkWidget();
    if( column != null && column.isDisposed() ) {
      error( SWT.ERROR_INVALID_ARGUMENT );
    }
    if( column == sortColumn ) {
      return;
    }
    if( sortColumn != null && !sortColumn.isDisposed() ) {
      sortColumn.setSortDirection( SWT.NONE );
    }
    sortColumn = column;
    if( sortColumn != null ) {
      sortColumn.setSortDirection( sortDirection );
    }
  }

  /**
   * Sets the direction of the sort indicator for the receiver. The value can be
   * one of <code>UP</code>, <code>DOWN</code> or <code>NONE</code>.
   *
   * @param direction the direction of the sort indicator
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public void setSortDirection( int direction ) {
    checkWidget();
    if( direction != SWT.UP && direction != SWT.DOWN && direction != SWT.NONE )
    {
      return;
    }
    sortDirection = direction;
    if( sortColumn == null || sortColumn.isDisposed() ) {
      return;
    }
    sortColumn.setSortDirection( sortDirection );
  }

  /**
   * Returns the column which shows the sort indicator for the receiver. The
   * value may be null if no column shows the sort indicator.
   *
   * @return the sort indicator
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see #setSortColumn(TreeColumn)
   */
  public TreeColumn getSortColumn() {
    checkWidget();
    return sortColumn;
  }

  /**
   * Returns the direction of the sort indicator for the receiver. The value
   * will be one of <code>UP</code>, <code>DOWN</code> or <code>NONE</code>.
   *
   * @return the sort direction
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see #setSortDirection(int)
   */
  public int getSortDirection() {
    checkWidget();
    return sortDirection;
  }

  /**
   * Returns an array of zero-relative integers that map the creation order of
   * the receiver's items to the order in which they are currently being
   * displayed.
   * <p>
   * Specifically, the indices of the returned array represent the current
   * visual order of the items, and the contents of the array represent the
   * creation order of the items.
   * </p>
   * <p>
   * Note: This is not the actual structure used by the receiver to maintain its
   * list of items, so modifying the array will not affect the receiver.
   * </p>
   *
   * @return the current visual order of the receiver's items
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see Tree#setColumnOrder(int[])
   * @see TreeColumn#getMoveable()
   * @see TreeColumn#setMoveable(boolean)
   * @see SWT#Move
   */
  public int[] getColumnOrder() {
    checkWidget();
    int[] result;
    if( columnHolder.size() == 0 ) {
      result = new int[ 0 ];
    } else {
      result = new int[ columnOrder.length ];
      System.arraycopy( columnOrder, 0, result, 0, columnOrder.length );
    }
    return result;
  }

  ///////////////////////////////////////
  // Listener registration/deregistration

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * the receiver's selection changes, by sending it one of the messages defined
   * in the <code>SelectionListener</code> interface.
   * <p>
   * When <code>widgetSelected</code> is called, the item field of the event
   * object is valid. If the receiver has <code>SWT.CHECK</code> style set and
   * the check selection changes, the event object detail field contains the
   * value <code>SWT.CHECK</code>. <code>widgetDefaultSelected</code> is
   * typically called when an item is double-clicked. The item field of the
   * event object is valid for default selection, but the detail field is not
   * used.
   * </p>
   *
   * @param listener the listener which should be notified
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SelectionListener
   * @see #removeSelectionListener
   * @see SelectionEvent
   */
  public void addSelectionListener( SelectionListener listener ) {
    checkWidget();
    SelectionEvent.addListener( this, listener );
  }

  /**
   * Removes the listener from the collection of listeners who will be notified
   * when the receiver's selection changes.
   *
   * @param listener the listener which should no longer be notified
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SelectionListener
   * @see #addSelectionListener
   */
  public void removeSelectionListener( SelectionListener listener ) {
    checkWidget();
    SelectionEvent.removeListener( this, listener );
  }

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * an item in the receiver is expanded or collapsed by sending it one of the
   * messages defined in the <code>TreeListener</code> interface.
   *
   * @param listener the listener which should be notified
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see TreeListener
   * @see #removeTreeListener
   */
  public void addTreeListener( TreeListener listener ) {
    checkWidget();
    TreeEvent.addListener( this, listener );
  }

  /**
   * Removes the listener from the collection of listeners who will be notified
   * when items in the receiver are expanded or collapsed.
   *
   * @param listener the listener which should no longer be notified
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see TreeListener
   * @see #addTreeListener
   */
  public void removeTreeListener( TreeListener listener ) {
    checkWidget();
    TreeEvent.removeListener( this, listener );
  }

  /////////////////////////////////
  // Methods to cleanup on dispose

  void releaseWidget() {
    if( resizeListener != null ) {
      removeControlListener( resizeListener );
    }
    super.releaseWidget();
  }

  void releaseChildren() {
    TreeItem[] items = getItems();
    for( int i = 0; i < items.length; i++ ) {
      items[ i ].dispose();
    }
    TreeColumn[] cols = columnHolder.getItems();
    for( int c = 0; c < cols.length; c++ ) {
      cols[ c ].dispose();
      columnHolder.remove( cols[ c ] );
    }
    super.releaseChildren();
  }

  void removeFromSelection( TreeItem item ) {
    int index = -1;
    for( int i = 0; index == -1 && i < selection.length; i++ ) {
      if( selection[ i ] == item ) {
        index = i;
      }
    }
    if( index != -1 ) {
      TreeItem[] newSelection = new TreeItem[ selection.length - 1 ];
      System.arraycopy( selection, 0, newSelection, 0, index );
      if( index < selection.length - 1 ) {
        int length = selection.length - index - 1;
        System.arraycopy( selection, index + 1, newSelection, index, length );
      }
      selection = newSelection;
    }
  }

  /////////////////////
  // Widget dimensions

  public Point computeSize( int wHint, int hHint, boolean changed ) {
    checkWidget();
    int width = 0;
    int height = 0;
    if( getColumnCount() > 0 ) {
      for( int i = 0; i < getColumnCount(); i++ ) {
        width += getColumn( i ).getWidth();
      }
    } else {
      for( int i = 0; i < getItemCount(); i++ ) {
        TreeItem item = getItem( i );
        if( item.isCached() ) {
          int itemWidth = item.getPreferredWidth( 0, false );
          width = Math.max( width, itemWidth );
          if( item.getExpanded() ) {
            int innerWidth = getMaxInnerWidth( item.getItems(), 1 );
            width = Math.max( width, innerWidth );
          }
        }
      }
    }
    height += getHeaderHeight();
    height += getItemCount() * getItemHeight();
    for( int i = 0; i < getItemCount(); i++ ) {
      TreeItem item = getItem( i );
      if( !item.isInDispose() && item.getExpanded() ) {
        height += item.getInnerHeight();
      }
    }
    if( width == 0 ) {
      width = DEFAULT_WIDTH;
    }
    if( height == 0 ) {
      height = DEFAULT_HEIGHT;
    }
    if( wHint != SWT.DEFAULT ) {
      width = wHint;
    }
    if( hHint != SWT.DEFAULT ) {
      height = hHint;
    }
    int border = getBorderWidth();
    width += border * 2;
    height += border * 2;
    if( ( style & SWT.V_SCROLL ) != 0 ) {
      width += verticalBar.getSize().x;
    }
    if( ( style & SWT.H_SCROLL ) != 0 ) {
      height += horizontalBar.getSize().y;
    }
    return new Point( width, height );
  }

  /////////////////////
  // item layout helper

  private int getMaxInnerWidth( TreeItem[] items, int level ) {
    int maxInnerWidth = 0;
    for( int i = 0; i < items.length; i++ ) {
      if( items[ i ] != null && items[ i ].isCached() ) {
        int indention = level * getIndentionWidth(); // TODO [tb] : test
        int itemWidth = items[ i ].getPreferredWidth( 0, false ) + indention;
        maxInnerWidth = Math.max( maxInnerWidth, itemWidth );
        if( items[ i ].getExpanded() ) {
          int innerWidth = getMaxInnerWidth( items[ i ].getItems(), level + 1 );
          maxInnerWidth = Math.max( maxInnerWidth, innerWidth );
        }
      }
    }
    return maxInnerWidth;
  }

  int getCellLeft( int index ) {
    return getColumnCount() == 0 ? 0 : getColumn( index ).getLeft();
  }

  private int getCellWidth( int index ) {
    return   getColumnCount() == 0 && index == 0
           ? getMaxInnerWidth( getItems(), 1 )
           : getColumn( index ).getWidth();
  }

  int getImageOffset( int index ) {
    // Note: The left cell-padding is visually ignored for the tree-column
    int result = isTreeColumn( index ) ? 0 : getCellPadding().x;
    if( hasCheckBoxes( index ) ) {
      result += getCheckImageOuterSize().x;
    }
    return result;
  }

  private int getTextOffset( int index ) {
    int result = getImageOffset( index );
    result += getItemImageOuterWidth( index );
    if( isTreeColumn( index ) ) {
      result += TEXT_MARGIN.x;
    }
    return result;
  }

  int getTextWidth( int index ) {
    int result = getCellWidth( index ) - getTextOffset( index );
    if( isTreeColumn( index ) ) {
      result -= ( TEXT_MARGIN.width - TEXT_MARGIN.x );
    }
    return Math.max( 0, result );
  }

  int getIndentionOffset( TreeItem item ) {
    return this.getIndentionWidth() * ( item.depth + 1);
  }

  int getVisualCellLeft( int index, TreeItem item ) {
    int result = getCellLeft( index ) - scrollLeft;
    if( isTreeColumn( index ) ) {
      result += getIndentionOffset( item );
    }
    if( hasCheckBoxes( index ) ) {
      result += getCheckImageOuterSize().x;
    }
    return result;
  }

  int getVisualCellWidth( int index, TreeItem item ) {
    int result;
    if( getColumnCount() == 0 && index == 0 ) {
      String text = item.getText( 0 );
      int textWidth = Graphics.stringExtent( item.getFont(), text ).x;
      result = getCellPadding().width
               + getItemImageOuterWidth( index )
               + textWidth
               + TEXT_MARGIN.width;
    } else {
      result = getColumn( index ).getWidth();
      if( isTreeColumn( index ) ) {
        result -= getIndentionOffset( item );
      }
      if( hasCheckBoxes( index ) ) {
        result -= getCheckImageOuterSize().x;
      }
      result = Math.max( 0, result );
    }
    return result;
  }

  int getVisualTextLeft( int index, TreeItem item ) {
    return getVisualCellLeft( index, item ) + getCellPadding().x + getItemImageOuterWidth( index );
  }

  int getVisualTextWidth( int index, TreeItem item ) {
    int result = 0;
    if( index == 0 && getColumnCount() == 0 ) {
      result = Graphics.stringExtent( item.getFont(), item.getText( 0 ) ).x;
      result += TEXT_MARGIN.width;
    } else if( index >= 0 && index < getColumnCount() ) {
      result = getTextWidth( index ) - getIndentionOffset( item );
      result = Math.max( 0, result );
    }
    return result;
  }

  int getPreferredCellWidth( TreeItem item, int columnIndex, boolean checkData ) {
    int result = item.getPreferredWidthBuffer( columnIndex );
    if( !item.hasPreferredWidthBuffer( columnIndex ) ) {
      result = getTextOffset( columnIndex ) ;
      Rectangle padding = getCellPadding();
      result += Graphics.stringExtent( getFont(), item.getTextWithoutMaterialize( columnIndex ) ).x;
      result += padding.width - padding.x;
      if( isTreeColumn( columnIndex ) ) {
        result += TEXT_MARGIN.width - TEXT_MARGIN.x;
      }
      item.setPreferredWidthBuffer( columnIndex, result );
    }
    return result;
  }

  boolean isTreeColumn( int index ) {
    return    index == 0 && getColumnCount() == 0
           || getColumnCount() > 0 && getColumnOrder()[ 0 ] == index;
  }

  private boolean hasCheckBoxes( int index ) {
    return ( style & SWT.CHECK ) != 0 && isTreeColumn( index );
  }

  private boolean hasColumnImages( int columnIndex ) {
    int count = columnIndex == 0 ? itemImageCount : getColumn( columnIndex ).itemImageCount;
    return count > 0;
  }

  void updateColumnImageCount( int columnIndex, Image oldImage, Image newImage ) {
    int delta = 0;
    if( oldImage == null && newImage != null ) {
      delta = +1;
    } else if( oldImage != null && newImage == null ) {
      delta = -1;
    }
    if( delta != 0 ) {
      if( columnIndex == 0 ) {
        itemImageCount += delta;
      } else {
        TreeColumn column = getColumn( columnIndex );
        column.itemImageCount += delta;
      }
    }
  }

  void updateItemImageSize( Image image ) {
    if( image != null && itemImageSize == null ) {
      Rectangle imageBounds = image.getBounds();
      itemImageSize = new Point( imageBounds.width, imageBounds.height );
    }
  }

  Point getItemImageSize( int index ) {
    Point result;
    if( hasColumnImages( index ) ) {
      result = getItemImageSize();
      if( getColumnCount() > 0 ) {
        int availWidth = getColumn( index ).getWidth();
        availWidth -= getCellPadding().x;
        availWidth = Math.max( 0, availWidth );
        result.x = Math.min( result.x, availWidth );
      }
    } else {
      result = new Point( 0, 0 );
    }
    return result;
  }

  private int getItemImageOuterWidth( int index ) {
    int result = 0;
    if( hasColumnImages( index ) ) {
      result += getItemImageSize( index ).x;
      result += getCellSpacing();
    }
    return result;
  }

  private Point getItemImageSize() {
    Point result = new Point( 0, 0 );
    if( itemImageSize != null ) {
      result.x = itemImageSize.x;
      result.y = itemImageSize.y;
    }
    return result;
  }

  private Point getCheckImageSize() {
    return getThemeAdapter().getCheckBoxImageSize( this );
  }

  private TreeThemeAdapter getThemeAdapter() {
    return ( TreeThemeAdapter )getAdapter( IThemeAdapter.class );
  }

  private Point getCheckImageOuterSize() {
    Point result = getCheckImageSize();
    Rectangle margin = getCheckBoxMargin();
    result.x += margin.width;
    result.y += margin.height;
    return result;
  }

  private Rectangle getCheckBoxMargin() {
    if( bufferedCheckBoxMargin == null ) {
      bufferedCheckBoxMargin = getThemeAdapter().getCheckBoxMargin( this );
    }
    return bufferedCheckBoxMargin;
  }

  private int getIndentionWidth() {
    return getThemeAdapter().getIndentionWidth( this );
  }

  ///////////////////
  // Helping methods


  void checkAllData() {
    // TODO [tb] : call only in doRedrawFake?
    updateFlatIndices();
    for( int i = 0; i < itemCount; i++ ) {
      checkDataRecursively( items[ i ], i );
    }
  }
  
  private void checkDataRecursively( TreeItem item, int index ) {
    if( isItemVisible( item ) ) {
      checkData( item, index );
      if( item.getExpanded() ) {
        int itemCount = item.getItemCount();
        for( int i = 0; i < itemCount; i++ ) {
          // TODO this could materialize items
          TreeItem subItem = item.getItem( i );
          checkDataRecursively( subItem, i );
        }
      }
    }
  }

  private boolean isItemVisible( TreeItem item ) {
    boolean result = false;
    int itemPosition = item.getItemTop();
    if( itemPosition >= 0 && itemPosition <= getSize().y ) {
      TreeItem parentItem = item.getParentItem();
      if( parentItem != null ) {
        if( parentItem.getExpanded() ) {
          result = true;
        }
      } else {
        result = true;
      }
    }
    return result;
  }

  // TODO [bm]: performance impact - replace this with logic to only partly
  // update the flat indices when there are changes in the visibility hierarchy
  // like new items, removed items, expand/
  // TODO [tb] : alternative: Only index when needed.
  /* package */void updateFlatIndices() {
    int flatIndex = 0;
    TreeItem[] uItems = getItems();
    for( int i = 0; i < uItems.length; i++ ) {
      TreeItem treeItem = uItems[ i ];
      treeItem.flatIndex = flatIndex;
      flatIndex++;
      flatIndex = updateFlatIndicesSub( treeItem, flatIndex );
    }
  }

  private int updateFlatIndicesSub( TreeItem item, int flatIndex ) {
    int newFlatIndex = flatIndex;
    if( item.getExpanded() ) {
      TreeItem[] subItems = item.getItems();
      for( int i = 0; i < subItems.length; i++ ) {
        TreeItem subItem = subItems[ i ];
        subItem.flatIndex = newFlatIndex;
        newFlatIndex++;
        newFlatIndex = updateFlatIndicesSub( subItem, newFlatIndex );
      }
    }
    return newFlatIndex;
  }

  final boolean checkData( TreeItem item, int index ) {
    boolean result = true;
    if( isVirtual() && !item.isCached() ) {
      item.markCached();
      SetDataEvent event = new SetDataEvent( Tree.this, item, index );
      event.processEvent();
      // widget could be disposed at this point
      if( isDisposed() || item.isDisposed() ) {
        result = false;
      }
    }
    return result;
  }

  private static int checkStyle( int style ) {
    int result = style;
    if( ( style & SWT.NO_SCROLL ) == 0 ) {
      result |= SWT.H_SCROLL | SWT.V_SCROLL;
    }
    return checkBits( result, SWT.SINGLE, SWT.MULTI, 0, 0, 0, 0 );
  }

  Rectangle getCellPadding() {
    if( bufferedCellPadding == null ) {
      bufferedCellPadding = getThemeAdapter().getCellPadding( this );
    }
    return bufferedCellPadding;
  }

  int getCellSpacing() {
    if( bufferedCellSpacing < 0 ) {
      bufferedCellSpacing = getThemeAdapter().getCellSpacing( this );
    }
    return bufferedCellSpacing;
  }

  /////////////
  // ScrollBars

  // TODO [if] move to Scrollable as in SWT
  private void createScrollBars() {
    if( ( style & SWT.H_SCROLL ) != 0 ) {
      horizontalBar = new ScrollBar( this, SWT.H_SCROLL );
      horizontalBar.setVisible( false );
    }
    if( ( style & SWT.V_SCROLL ) != 0 ) {
      verticalBar = new ScrollBar( this, SWT.V_SCROLL );
      verticalBar.setVisible( false );
    }
  }

  /**
   * Returns the receiver's horizontal scroll bar if it has
   * one, and null if it does not.
   *
   * @return the horizontal scroll bar (or null)
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.4
   */
  // TODO [if] move to Scrollable as in SWT
  public ScrollBar getHorizontalBar() {
    checkWidget();
    return horizontalBar;
  }

  /**
   * Returns the receiver's vertical scroll bar if it has
   * one, and null if it does not.
   *
   * @return the vertical scroll bar (or null)
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @since 1.4
   */
  //  TODO [if] move to Scrollable as in SWT
  public ScrollBar getVerticalBar() {
    checkWidget();
    return verticalBar;
  }

  ///////////////////////////////////////
  // Helping methods - dynamic scrollbars

  boolean hasVScrollBar() {
    return hasVScrollBar;
  }

  boolean hasHScrollBar() {
    return hasHScrollBar;
  }

  int getVScrollBarWidth() {
    int result = 0;
    if( hasVScrollBar() ) {
      result = verticalBar.getSize().x;
    }
    return result;
  }

  int getHScrollBarHeight() {
    int result = 0;
    if( hasHScrollBar() ) {
      result = horizontalBar.getSize().y;
    }
    return result;
  }

  boolean needsVScrollBar() {
    int availableHeight = getClientArea().height;
    int height = getHeaderHeight();
    height += getItemCount() * getItemHeight();
    for( int i = 0; i < getItemCount(); i++ ) {
      TreeItem item = getItem( i );
      if( item.getExpanded() ) {
        height += item.getInnerHeight();
      }
    }
    return height > availableHeight;
  }

  boolean needsHScrollBar() {
    boolean result = false;
    int availableWidth = getClientArea().width;
    int columnCount = getColumnCount();
    if( columnCount > 0 ) {
      int totalWidth = 0;
      for( int i = 0; i < columnCount; i++ ) {
        TreeColumn column = getColumn( i );
        totalWidth += column.getWidth();
      }
      result = totalWidth > availableWidth;
    } else {
      int maxWidth = 0;
      for( int i = 0; i < getItemCount(); i++ ) {
        TreeItem item = getItem( i );
        if( item != null && !item.isInDispose() && item.isCached() ) {
          int itemWidth = item.getPreferredWidth( 0, false );
          maxWidth = Math.max( maxWidth, itemWidth );
          if( item.getExpanded() ) {
            int innerWidth = getMaxInnerWidth( item.getItems(), 1 );
            maxWidth = Math.max( maxWidth, innerWidth );
          }
        }
      }
      result = maxWidth > availableWidth;
    }
    return result;
  }

  void updateScrollBars() {
    if( ( style & SWT.NO_SCROLL ) == 0 ) {
      hasVScrollBar = false;
      hasHScrollBar = needsHScrollBar();
      if( needsVScrollBar() ) {
        hasVScrollBar = true;
        hasHScrollBar = needsHScrollBar();
      }
      horizontalBar.setVisible( hasHScrollBar );
      verticalBar.setVisible( hasVScrollBar );
    }
  }

  ///////////////////
  // Skinning support

  void reskinChildren( int flags ) {
    TreeItem[] items = getItems();
    if( items != null ) {
      for( int i = 0; i < items.length; i++ ) {
        TreeItem item = items[ i ];
        if( item != null ) {
          item.reskinChildren( flags );
        }
      }
    }
    TreeColumn[] columns = getColumns();
    if( columns != null ) {
      for( int i = 0; i < columns.length; i++ ) {
        TreeColumn column = columns[ i ];
        if( !column.isDisposed() ) {
          column.reskinChildren( flags );
        }
      }
    }
    super.reskinChildren( flags );
  }

  boolean isVirtual() {
    return ( style & SWT.VIRTUAL ) != 0;
  }

  void createItem( TreeItem item, int index ) {
    if( itemCount == items.length ) {
      /*
       * Grow the array faster when redraw is off or the table is not visible.
       * When the table is painted, the items array is resized to be smaller to
       * reduce memory usage.
       */
      boolean small = /* drawCount == 0 && */isVisible();
      int length = small ? items.length + 4 : Math.max( 4, items.length * 3 / 2 );
      TreeItem[] newItems = new TreeItem[ length ];
      System.arraycopy( items, 0, newItems, 0, items.length );
      items = newItems;
    }
    System.arraycopy( items, index, items, index + 1, itemCount - index );
    items[ index ] = item;
    itemCount++;
    adjustItemIndices( index );
  }

  void destroyItem( TreeItem treeItem, int index ) {    
    itemCount--;
    if( itemCount == 0 ) {
      setTreeEmpty();
    } else {
      System.arraycopy( items, index + 1, items, index, itemCount - index );
      items[ itemCount ] = null;
    }
    adjustItemIndices( index );
  }
  
  private void adjustItemIndices( int start ) {
    for( int i = start; i < itemCount; i++ ) {
      if( items[ i ] != null ) {
        items[ i ].index = i;
      }
    }
  }
}
