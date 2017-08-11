package hipad.coapservice;

import com.orhanobut.logger.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class ZHDataObserve extends CoapResource {
	private String msg="";
	
	public ZHDataObserve(String name) {
		super(name);
		setObservable(true); // enable observing
		setObserveType(Type.CON); // configure the notification type to CONs
		getAttributes().setObservable(); // mark observable in the Link-Format
	}
	public  void change(String msg){
		this.msg=msg;
		if(getObserverCount()>0){
			changed();
		}
	}
	@Override
	public void handleGET(CoapExchange exchange) {
		Logger.d("get:" + exchange.getRequestText());
		exchange.respond(ResponseCode.CONTENT,msg,MediaTypeRegistry.TEXT_PLAIN);
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		Logger.d("post:" + exchange.getRequestText());
		exchange.respond(ResponseCode.CONTENT, "{\"code\":200,\"msg\":\"ok\"}", MediaTypeRegistry.APPLICATION_JSON);
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		Logger.d("put:" + exchange.getRequestText());
		exchange.respond(ResponseCode.CONTENT, "{\"code\":200,\"msg\":\"ok\"}", MediaTypeRegistry.APPLICATION_JSON);
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		Logger.d("delete:" + exchange.getRequestText());
		super.handleDELETE(exchange);
	}



}
