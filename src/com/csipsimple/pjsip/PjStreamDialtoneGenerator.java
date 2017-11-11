/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.pjsip;

import com.csipsimple.utils.Log;

import org.pjsip.pjsua.pj_pool_t;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_port;
import org.pjsip.pjsua.pjmedia_tone_digit;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_call_info;

/**
 * DTMF In band tone generator for a given call object
 * It creates it's own pool, media port, and can stream in. 
 * @author r3gis3r
 *
 */
public class PjStreamDialtoneGenerator {

    
    private static final String THIS_FILE = "PjStreamDialtoneGenerator";
    private static String SUPPORTED_DTMF = "0123456789abcd*#";
    private final int callId;
	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	
	
	public PjStreamDialtoneGenerator(int aCallId) {
        callId = aCallId;
    }
	
	/**
	 * Start the tone generate.
	 * This is automatically done by the send dtmf
	 * @return the pjsip error code for creation
	 */
	private synchronized int startDialtoneGenerator() {
		
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(callId, info);
		int status;
		
		dialtonePool = pjsua.pjsua_pool_create("tonegen-"+callId, 512, 512);
		pj_str_t name = pjsua.pj_str_copy("dialtoneGen");
		long clockRate = 8000;
		long channelCount = 1;
		long samplesPerFrame = 160;
		long bitsPerSample = 16;
		long options = 0;
		int[] dialtoneSlotPtr = new int[1];
		dialtoneGen = new pjmedia_port();
		status = pjsua.pjmedia_tonegen_create2(dialtonePool, name, clockRate, channelCount, samplesPerFrame, bitsPerSample, options, dialtoneGen);
		if (status != pjsua.PJ_SUCCESS) {
			stopDialtoneGenerator();
			return status;
		}
		status = pjsua.conf_add_port(dialtonePool, dialtoneGen, dialtoneSlotPtr);
		if (status != pjsua.PJ_SUCCESS) {
			stopDialtoneGenerator();
			return status;
		}
		dialtoneSlot = dialtoneSlotPtr[0];
		int callConfSlot = info.getConf_slot();
		status = pjsua.conf_connect(dialtoneSlot, callConfSlot);
		if (status != pjsua.PJ_SUCCESS) {
			dialtoneSlot = -1;
			stopDialtoneGenerator();
			return status;
		}
		return pjsua.PJ_SUCCESS;
		
	}

	/**
	 * Stop the dialtone generator.
	 * This has to be called manually when no more DTMF codes are to be send for the associated call
	 */
	public synchronized void stopDialtoneGenerator() {
	    stopSending();
		// Destroy the port
		if (dialtoneSlot != -1) {
			pjsua.conf_remove_port(dialtoneSlot);
			dialtoneSlot = -1;
		}
		
        if (dialtoneGen != null) {
            pjsua.pjmedia_port_destroy(dialtoneGen);
            dialtoneGen = null;
        }

		if (dialtonePool != null) {
			pjsua.pj_pool_release(dialtonePool);
			dialtonePool = null;
		}
	}
	
	/**
	 * Send multiple tones.
	 * @param dtmfChars tones list to send. 
	 * @return the pjsip status
	 */
	public synchronized int sendPjMediaDialTone(String dtmfChars) {
		if (dialtoneGen == null) {
			int status = startDialtoneGenerator();
			if (status != pjsua.PJ_SUCCESS) {
				return -1;
			}
		}
		int status = pjsuaConstants.PJ_SUCCESS;
		stopSending();
		
		for(int i = 0 ; i < dtmfChars.length(); i++ ) {
		    char d = dtmfChars.charAt(i);
		    if(SUPPORTED_DTMF.indexOf(d) == -1) {
		        Log.w(THIS_FILE, "Unsupported DTMF char " + d);
		    } else {
		        // Found dtmf char, use digit api
		        pjmedia_tone_digit[] tone = new pjmedia_tone_digit[1];
		        tone[0] = new pjmedia_tone_digit();
                tone[0].setVolume((short) 0);
                tone[0].setOn_msec((short) 100);
                tone[0].setOff_msec((short) 200);
                tone[0].setDigit(d);
                pjsua.pjmedia_tonegen_play_digits(dialtoneGen, 1, tone, 0);
		    }
		}

		return status;
	}
	
	private void stopSending() {
        if (dialtoneGen != null) {
            pjsua.pjmedia_tonegen_stop(dialtoneGen);
        }
	}

}
