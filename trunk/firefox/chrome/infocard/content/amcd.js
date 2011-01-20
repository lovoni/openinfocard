  var amcd = {
    "token" : {
      "type" : "samlv1:bearer",
      "cond" : {"requireAppliesTo":true},
      "to"   : {
        "url" : "https://rp.example.com/rp",
        "method" : "POST"
      },
      "requiredclaims" : [PPID], 
      "optionalclaims" : [], 
      "privacy" : [
        { "url" : "https://example.com/privacystatement.pdf", "lang" : "en"}
        { "url" : "https://example.com/datenschutzerkl%C3%84rung.pdf", "lang" : "de"}
      ]
      "issuers" : [
        {
          "urn"   : "urn:kantara:2010:google",
          "text"  : [
            {"lang" : "DE", "value" : "Google Deutschland"}
          ],
          "icons" : [
            { "w":16, "h":16, "url": "data:ABC...BA"},
            { "w":16, "h":32, "url": "data:UXW...YZ"}
          ]
        },
        {
          "urn"   : "https://facebook.com/",
          "text"  : [
            {"lang" : "DE", "value" : "facebook"}
          ],
          "icons" : [
            { "w":16, "h":16, "url": "data:ABC...BA", "t" : "image/jpeg"},
            { "w":16, "h":32, "url": "https://facebook.de/i16x32.png" }
          ]
        }
      ]
    } // end token
  };

  var amcd = {
      /* Information Card Profile */
      "http://docs.oasis-open.org/imi/ns/identity-200810" : {
        "cond" : {"requireAppliesTo":true},
        "to"   : {
          "url" : "https://xmldap.org/relyingparty/infocard",
          "method" : "POST"
        },
        "claimssets" : [
           { "set" : [{
             "name" : "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier",
             "alg" : "IMI1.0",
             "optional" : false, /* default: optional : true */
             "alias" : "PPID",
             "loa" : 1,
             "description" : "A private personal identifier (PPID) that identifies the subject to a relying party. The word \"private\" is used in the sense that the subject identifier is specific to a given relying party and hence private to that relying party. A subject's PPID at one relying party cannot be correlated with the subject's PPID at another relying party. Typically, the PPID should be generated by an identity provider as a pair-wise pseudonym for a subject for a given relying party. For a self-issued information card, the self-issued identity provider in an Identity Selector system should generate a PPID for each relying party as a function of the card identifier and the relying party&apos;s identity.",
           }]},
           { "set" : [{
             "name" : "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
             "optional" : false, /* default: optional : true */
             "alias" : "email",
             "loa" : 1,
             "description" : "An email address that ends in .edu",
             "valuematch" : "*\.edu$"
           }]},
           { "issuers" : [{"idref" : "urn:kantara:2010:visa"},
                          {"idref" : "https://mastercard.com/"}],
             "set" : 
             [{
             "name" : "http://schemas.xmlsoap.org/ws/2011/01/identity/claims/creditcardnumber",
             "optional" : false, /* default: optional : true */
             "alias" : "creditcardnumber",
             "loa" : 1,
             "description" : "Your creditcard number",
           },
           {
             "name" : "http://schemas.xmlsoap.org/ws/2011/01/identity/claims/creditcardvalidthru",
             "optional" : false, /* default: optional : true */
             "alias" : "creditcardvalidthru",
             "loa" : 1,
             "description" : "The expiration date of your creditcard",
           },
           {
             "name" : "http://schemas.xmlsoap.org/ws/2011/01/identity/claims/creditcardsecuritycode",
             "optional" : false, /* default: optional : true */
             "alias" : "creditcardsecuritycode",
             "loa" : 1,
             "description" : "The security code on the back of your creditcard",
           }
           ]}
           ],
        "privacy" : [
          { "url" : "https://example.com/privacystatement.pdf", "lang" : "en"},
          { "url" : "https://example.com/datenschutzerkl%C3%84rung.pdf", "lang" : "de"}
        ],
        "issuers" : [
          {
            "id"   : "urn:kantara:2010:visa",
            "text"  : [
              {"lang" : "DE", "value" : "Visa Deutschland"}
            ],
            "icons" : [
              { "w":16, "h":16, "url": "data:ABC...BA"},
              { "w":16, "h":32, "url": "data:UXW...YZ"}
            ]
          },
          {
            "id"   : "https://mastercard.com/",
            "text"  : [
              {"lang" : "DE", "value" : "Mastercard"}
            ],
            "icons" : [
              { "w":16, "h":16, "url": "data:ABC...BA", "t" : "image/jpeg"},
              { "w":16, "h":32, "url": "https://mastercard.com/i16x32.png" }
            ]
          }
        ]
      } // end token
    };


  var amcdstr = JSON.stringify(amcd);
  alert(amcdstr);
  alert(amcd.issuers[0].urn);