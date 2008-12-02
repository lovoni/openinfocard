//------------------------------------------------------------------------------
// Desc:
// Tabs: 3
//
// Copyright (c) 2007-2008 Novell, Inc. All Rights Reserved.
//
// This program and the accompanying materials are made available
// under, alternatively, the terms of:  a) the Eclipse Public License v1.0
// which accompanies this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html; or, b) the Apache License,
// Version 2.0 which accompanies this distribution as is available at
// www.opensource.org/licenses/apache2.0.php.
//
// To contact Novell about this file by physical or electronic mail,
// you may find current contact information at www.novell.com.
//
// Author: Andrew Hodgkinson <ahodgkinson@novell.com>
//------------------------------------------------------------------------------

// **************************************************************************
// Desc: Global services
// **************************************************************************

var gPrefService = Components.classes[
                "@mozilla.org/preferences-service;1"].
                        getService( Components.interfaces.nsIPrefBranch);

// **************************************************************************
// Desc:
// **************************************************************************

var IdentitySelectorPrefs =
{
        // ***********************************************************************
        // Method: hasPref
        // ***********************************************************************
       
        hasPref : function( componentId, prefId)
        {
                var fullPrefId = "extensions." + componentId + "." + prefId;
               
                if( gPrefService.getPrefType( fullPrefId) != gPrefService.PREF_INVALID)
                {
                        return( true);
                }
               
                return( false);
        },
       
        // ***********************************************************************
        // Method: getStringPref
        // ***********************************************************************
       
        getStringPref : function( componentId, prefId)
        {
                var fullPrefId = "extensions." + componentId + "." + prefId;
               
                if( gPrefService.getPrefType( fullPrefId) == gPrefService.PREF_STRING)
                {
                        return( gPrefService.getCharPref( fullPrefId));
                }
               
                return( null);
        },
       
        // ***********************************************************************
        // Method: setStringPref
        // ***********************************************************************
       
        setStringPref : function( componentId, prefId, prefValue)
        {
                var fullPrefId = "extensions." + componentId + "." + prefId;
                gPrefService.setCharPref( fullPrefId, prefValue);
        },
       
        // ***********************************************************************
        // Method: getBooleanPref
        // ***********************************************************************
       
        getBooleanPref : function( componentId, prefId)
        {
                var fullPrefId = "extensions." + componentId + "." + prefId;
               
                if( gPrefService.getPrefType( fullPrefId) == gPrefService.PREF_BOOL)
                {
                        return( gPrefService.getBoolPref( fullPrefId));
                }
               
                return( false);
        },
       
        // ***********************************************************************
        // Method: setBooleanPref
        // ***********************************************************************
       
        setBooleanPref : function( componentId, prefId, prefValue)
        {
                var fullPrefId = "extensions." + componentId + "." + prefId;
                gPrefService.setBoolPref( fullPrefId, prefValue);
        }
};