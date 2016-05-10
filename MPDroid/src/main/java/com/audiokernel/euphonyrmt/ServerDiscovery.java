package com.audiokernel.euphonyrmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import android.util.Log;

import org.xbill.DNS.Message;
import org.xbill.mDNS.Browse;
import org.xbill.mDNS.DNSSDListener;
import org.xbill.mDNS.MulticastDNSService;
import org.xbill.mDNS.ServiceInstance;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;

import com.audiokernel.euphonyrmt.tools.SettingsHelper;

public class ServerDiscovery {
    private static final String TAG = "ServerDiscovery";
	//The multicast lock we'll have to release
	private WifiManager.MulticastLock multicastLock;
	MulticastDNSService mdns;
	Object serviceDiscovery;
	public final List<ServerInfo> servers = new java.util.concurrent.CopyOnWriteArrayList<ServerInfo>() {
		public boolean add(ServerInfo info) {
			if (info.address == null)
				return false;
			int index = Collections.binarySearch(this, info, ServerInfo.byName);
			if (index < 0) index = ~index;
			else if (info.equals(this.get(index)))
				return false;
			Log.i(TAG, "Service Added: " + info);
			super.add(index, info);
			return true;
		}
	};
	public Runnable onChanged;
	final MPDApplication app;

	public ServerDiscovery(Context/*MPDApplication*/ context) {
		app = (MPDApplication) context;
		try {
			//By default, the android wifi stack will ignore broadcasts, fix that
			WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			multicastLock = wm.createMulticastLock("mupeace_bonjour");
		} catch(Exception e) {
			Log.w(TAG, e);
		}

		final Handler handler = new Handler();

		new Runnable() {
			int delay = 10000;
		    public void run() {
		    	start();
		    	if (!servers.isEmpty())
		    		delay = 300000;
		    	handler.postDelayed(this, delay);
		    }
		}.run();
	}

	protected void start() {
		try {
			if (mdns == null)
				mdns = new MulticastDNSService();

			if (serviceDiscovery != null)
				try {
					mdns.stopServiceDiscovery(serviceDiscovery);
				} finally {
					serviceDiscovery = null;
				}

			Log.i(TAG,"Service discovery starting");

			serviceDiscovery = mdns.startServiceDiscovery(new Browse("_mpd._tcp.local."), new DNSSDListener() {
				public void serviceDiscovered(Object id, ServiceInstance info) {
					Log.i(TAG,"Service Discovered: " + info);
					ServerInfo server = new ServerInfo(info);
					if (server.address != null)
						addNewServer(server);
					if (onChanged != null)
						onChanged.run();
                    new SettingsHelper(app.oMPDAsyncHelper)
                            .setHostname(
                                    server.address,
                                    server.port
                            );
				}

				public void serviceRemoved(Object id, ServiceInstance info) {
					Log.i(TAG,"Service Removed: " + info + " " + servers.remove(new ServerInfo(info)));
					if (onChanged != null)
						onChanged.run();
				}

				public void handleException(Object id, Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}

				public void receiveMessage(Object arg0, Message arg1) {
				}    		
			});
		} catch(IOException e) {
			Log.w(TAG, e);
		}
	}

	private void addNewServer(ServerInfo newServer) {
		for (ServerInfo server:servers ) {
			if(server.address.equals(newServer.address) && server.port == newServer.port)
				return;
		}
		servers.add(newServer);
	}
	public void onPause() {
		if (multicastLock != null)
			multicastLock.release();
	}

	public void onResume() {
		if (multicastLock != null)
			// Ask android to allow us to get WiFi broadcasts
			multicastLock.acquire();
	}

	public void onDestroy() {
		try {
			if (serviceDiscovery != null)
				mdns.stopServiceDiscovery(serviceDiscovery);
			mdns.close();
			mdns = null;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}
}
