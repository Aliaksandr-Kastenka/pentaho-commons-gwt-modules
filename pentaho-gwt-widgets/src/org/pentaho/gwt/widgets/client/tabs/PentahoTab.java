/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.gwt.widgets.client.tabs;

import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.utils.ImageUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import org.pentaho.mantle.client.events.EventBusUtil;
import org.pentaho.mantle.client.events.ReportTabCloseEvent;
import org.pentaho.mantle.client.events.ReportTabCloseEventHandler;
import org.pentaho.mantle.client.messages.Messages;

public class PentahoTab extends SimplePanel {

  public static final String SELECTED = "selected";
  private static String statusUrl;
  private PentahoTabPanel tabPanel;
  private Widget content;
  protected Label label = new Label();
  private boolean solutionBrowserShowing;

  public PentahoTab( String text, String tooltip, PentahoTabPanel tabPanel, Widget content, boolean closeable ) {
    this.content = content;
    this.tabPanel = tabPanel;
    setStylePrimaryName( "pentaho-tabWidget" );
    sinkEvents( Event.ONDBLCLICK | Event.ONMOUSEUP );
    setupNativeHooks();    

    if ( closeable ) {
      final Image closeTabImage =
          ImageUtil
              .getThemeableImage( "pentaho-tabWidget-close", "pentaho-closebutton", "pentaho-imagebutton-disabled" );
      closeTabImage.addClickHandler( new ClickHandler() {
        public void onClick( ClickEvent event ) {
          event.getNativeEvent().stopPropagation();
          closeTab();
		  fireEvent();            
        }
      } );
      closeTabImage.addMouseOverHandler( new MouseOverHandler() {
        public void onMouseOver( MouseOverEvent event ) {
          closeTabImage.removeStyleName( "pentaho-imagebutton-disabled" );
          closeTabImage.addStyleName( "pentaho-imagebutton-hover" );
        }
      } );
      closeTabImage.addMouseOutHandler( new MouseOutHandler() {

        public void onMouseOut( MouseOutEvent event ) {
          closeTabImage.removeStyleName( "pentaho-imagebutton-hover" );
          closeTabImage.addStyleName( "pentaho-imagebutton-disabled" );
        }
      } );

      HorizontalPanel p = new HorizontalPanel();
      setupLabel( text, tooltip );
      p.add( label );
      p.add( closeTabImage );
      setWidget( p );
    } else {
      setupLabel( text, tooltip );
      setWidget( label );
    }
  }

  public void setupLabel( String text, String tooltip ) {
    label.setText( text );
    label.setTitle( tooltip );
    label.setStylePrimaryName( "pentaho-tabWidgetLabel" );
  }

  public Widget getContent() {
    return content;
  }

  public void setContent( Widget content ) {
    this.content = content;
  }

  protected PentahoTabPanel getTabPanel() {
    return tabPanel;
  }

  protected void setTabPanel( PentahoTabPanel tabPanel ) {
    this.tabPanel = tabPanel;
  }

  public void onBrowserEvent( Event event ) {
    if ( ( event.getTypeInt() & Event.ONDBLCLICK ) == Event.ONDBLCLICK ) {
      onDoubleClick( event );
    } else if ( event.getButton() == Event.BUTTON_RIGHT ) {
      onRightClick( event );
    } else if ( event.getButton() == Event.BUTTON_LEFT ) {
      if ( event.getEventTarget().toString().toLowerCase().indexOf( "image" ) == -1 ) {
        fireTabSelected();
      }
    }
    super.onBrowserEvent( event );
  }

  public void onDoubleClick( Event event ) {
  }

  public void onRightClick( Event event ) {
  }

  public void setSelected( boolean selected ) {
    if ( selected ) {
      addStyleDependentName( SELECTED );
    } else {
      removeStyleDependentName( SELECTED );
    }
  }

  public String getLabelText() {
    return label.getText();
  }

  public void setLabelText( String text ) {
    label.setText( text );
  }

  public void setLabelTooltip( String tooltip ) {
    label.setTitle( tooltip );
  }

  public String getLabelTooltip() {
    return label.getTitle();
  }

  protected void closeTab() {
    tabPanel.closeTab( this, true );
  }

  protected void fireTabSelected() {
    tabPanel.selectTab( this );
  }

  public boolean isSolutionBrowserShowing() {
    return solutionBrowserShowing;
  }

  public void setSolutionBrowserShowing( boolean solutionBrowserShowing ) {
    this.solutionBrowserShowing = solutionBrowserShowing;
  }

  public static void addHandler( final String cancelUrl ) {
    statusUrl = cancelUrl.replace( "/cancel", "/status" );
    EventBusUtil.EVENT_BUS.addHandler(ReportTabCloseEvent.TYPE, new ReportTabCloseEventHandler() {
      @Override
      public void onTabClose(ReportTabCloseEvent event) {

        RequestBuilder builder = new RequestBuilder( RequestBuilder.GET, cancelUrl );
          builder.setHeader( "Content-Type", "application/xml" );
          RequestCallback callback = new RequestCallback() {

            public void onError( Request request, Throwable exception ) {
              MessageDialogBox dialogBox =
                  new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
              dialogBox.center();
            }

            public void onResponseReceived( Request request, Response response ) {
            }
          };

          try {
            builder.sendRequest( "", callback ); //$NON-NLS-1$ //$NON-NLS-2$
          } catch ( RequestException e ) {
            e.printStackTrace();
          }
      }
    });
  }

  public static void fireEvent() {
    RequestBuilder builder = new RequestBuilder( RequestBuilder.GET, statusUrl );
    builder.setHeader( "Content-Type", "application/xml" );
    RequestCallback callback = new RequestCallback() {

      public void onError( Request request, Throwable exception ) {
        MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
        dialogBox.center();
      }

      public void onResponseReceived( Request request, Response response ) {
        ReportTabCloseEvent closeEvent = new ReportTabCloseEvent();
        closeEvent.setAction( PentahoTab.class.getName() );
        if ( response.getStatusCode() == Response.SC_OK ) {
          JSONObject jsonObject = (JSONObject) JSONParser.parseLenient( response.getText() );
          String status = jsonObject.get( "status" ).toString();
            if ( status.equals( "QUEUED" ) || status.equals( "WORKING" ) || status.equals( "CONTENT_AVAILABLE" ) ) {
              closeEvent.setMessage( "Close" );
              EventBusUtil.EVENT_BUS.fireEvent( closeEvent );
            }
        }
      }
    };

    try {
      builder.sendRequest( "", callback ); //$NON-NLS-1$ //$NON-NLS-2$
    } catch ( RequestException e ) {
      e.printStackTrace();
    }
  }

  public static native void setupNativeHooks() /*-{
    $wnd.mantle_addHandler = function (cancelUrl) {
      @org.pentaho.gwt.widgets.client.tabs.PentahoTab::addHandler(Ljava/lang/String;)(cancelUrl);
    }
  }-*/;
}
