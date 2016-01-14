package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.facebook.Response;
import com.facebook.model.GraphObject;
import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.InboxMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxAction extends FacebookActionImpl<Map<Contact, List<HeadboxMessage>>> {

    private static final String TAG = InboxAction.class.getSimpleName();

    public InboxAction(FacebookAuthenticator sessionManager) {

        super(sessionManager);
    }

    @Override
    protected String getGraphPath() {
        return InboxMapper.Properties.INBOX_PATH;
    }

    @Override
    protected Map<Contact, List<HeadboxMessage>> processResponse(Response response) {


        Map<Contact, List<HeadboxMessage>> contactMessagesMap = new HashMap<Contact, List<HeadboxMessage>>();

        List<GraphObject> graphObjects = FacebookUtils.mappedListFromResponse(
                response, GraphObject.class);

        for (GraphObject graphObject : graphObjects) {

            Map<Contact, List<HeadboxMessage>> messages = InboxMapper
                    .create(getMe(), graphObject);

            if (messages != null && messages.size() > 0 )
                for (Contact contact : messages.keySet()) {

                    List<HeadboxMessage> headboxMessages = messages.get(contact);
                    contactMessagesMap.put(contact, headboxMessages);
                }
        }

        return contactMessagesMap;
    }

    @Override
    protected Bundle getBundle() {
        Bundle bundle = FacebookUtils.buildInboxBundle();
        bundle.putString(InboxMapper.Properties.LIMIT, String.valueOf(getLimit()));
        return bundle;
    }
}