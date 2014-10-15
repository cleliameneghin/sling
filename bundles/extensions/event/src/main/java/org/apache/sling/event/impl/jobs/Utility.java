/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.event.impl.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;

public abstract class Utility {

    public static final String PROPERTY_LOCK_CREATED = "lock.created";
    public static final String PROPERTY_LOCK_CREATED_APP = "lock.created.app";
    public static final String RESOURCE_TYPE_LOCK = "slingevent:Lock";

    public static final String TOPIC_STOPPED = "org/apache/sling/event/impl/jobs/STOPPED";
    public static final String TOPIC_STOP = "org/apache/sling/event/impl/jobs/STOP";
    public static final String PROPERTY_ID = "id";

    /**
     * Check the job topic.
     * @return <code>null</code> if the topic is correct, otherwise an error description is returned
     */
    public static String checkJobTopic(final Object jobTopic) {
        final String message;
        if ( jobTopic != null ) {
            if ( jobTopic instanceof String ) {
                boolean topicIsCorrect = false;
                try {
                    new Event((String)jobTopic, (Dictionary<String, Object>)null);
                    topicIsCorrect = true;
                } catch (final IllegalArgumentException iae) {
                    // we just have to catch it
                }
                if ( !topicIsCorrect ) {
                    message = "Discarding job - job has an illegal job topic";
                } else {
                    message = null;
                }
            } else {
                message = "Discarding job - job topic is not of type string";
            }
        } else {
            message = "Discarding job - job topic is missing";
        }
        return message;
    }

    /**
     * Check the job.
     * @return <code>null</code> if the topic topic is correct and all properties are serializable,
     *                           otherwise an error description is returned
     */
    public static String checkJob(final Object jobTopic, final Map<String, Object> properties) {
        final String msg = checkJobTopic(jobTopic);
        if ( msg == null ) {
            if ( properties != null ) {
                for(final Object val : properties.values()) {
                    if ( val != null && !(val instanceof Serializable) ) {
                        return "Discarding job - properties must be serializable: " + jobTopic + " : " + properties;
                    }
                }
            }
        }
        return msg;
    }

    /** Event property containing the time for job start and job finished events. */
    public static final String PROPERTY_TIME = "time";

    /**
     * Helper method for sending the notification events.
     */
    public static void sendNotification(final EventAdmin eventAdmin,
            final String eventTopic,
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final Long time) {
        if ( eventAdmin != null ) {
            // create job object
            final Map<String, Object> jobProps;
            if ( jobProperties == null ) {
                jobProps = new HashMap<String, Object>();
            } else {
                jobProps = jobProperties;
            }
            final Job job = new JobImpl(jobTopic, jobName, "<unknown>", jobProps);
            sendNotificationInternal(eventAdmin, eventTopic, job, time);
        }
    }

    /**
     * Helper method for sending the notification events.
     */
    public static void sendNotification(final EventAdmin eventAdmin,
            final String eventTopic,
            final Job job,
            final Long time) {
        if ( eventAdmin != null ) {
            // create new copy of job object
            final Job jobCopy = new JobImpl(job.getTopic(), job.getName(), job.getId(), ((JobImpl)job).getProperties());
            sendNotificationInternal(eventAdmin, eventTopic, jobCopy, time);
        }
    }

    /**
     * Helper method for sending the notification events.
     */
    private static void sendNotificationInternal(final EventAdmin eventAdmin,
            final String eventTopic,
            final Job job,
            final Long time) {
        final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
        // add basic job properties
        eventProps.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_ID, job.getId());
        eventProps.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC, job.getTopic());
        if ( job.getName() != null ) {
            eventProps.put(JobUtil.NOTIFICATION_PROPERTY_JOB_NAME, job.getName());
        }
        // copy payload
        for(final String name : job.getPropertyNames()) {
            eventProps.put(name, job.getProperty(name));
        }
        // remove async handler
        eventProps.remove(JobConsumer.PROPERTY_JOB_ASYNC_HANDLER);
        // add timestamp
        eventProps.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
        // add internal time information
        if ( time != null ) {
            eventProps.put(PROPERTY_TIME, time);
        }
        // compatibility:
        eventProps.put(JobUtil.PROPERTY_NOTIFICATION_JOB, toEvent(job));
        eventAdmin.postEvent(new Event(eventTopic, eventProps));
    }

    /**
     * Create an event from a job
     * @param job The job
     * @return New event object.
     */
    public static Event toEvent(final Job job) {
        final Map<String, Object> eventProps = new HashMap<String, Object>();
        eventProps.putAll(((JobImpl)job).getProperties());
        if ( job.getName() != null ) {
            eventProps.put(ResourceHelper.PROPERTY_JOB_NAME, job.getName());
        }
        eventProps.put(ResourceHelper.PROPERTY_JOB_ID, job.getId());
        eventProps.remove(JobConsumer.PROPERTY_JOB_ASYNC_HANDLER);
        return new Event(job.getTopic(), eventProps);
    }

    /**
     * Append properties to the string builder
     */
    private static void appendProperties(final StringBuilder sb,
            final Map<String, Object> properties) {
        if ( properties != null ) {
            sb.append(", properties=");
            boolean first = true;
            for(final String propName : properties.keySet()) {
                if ( propName.equals(ResourceHelper.PROPERTY_JOB_ID)
                    || propName.equals(ResourceHelper.PROPERTY_JOB_NAME)
                    || propName.equals(ResourceHelper.PROPERTY_JOB_TOPIC) ) {
                   continue;
                }
                if ( first ) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(propName);
                sb.append('=');
                final Object value = properties.get(propName);
                // the toString() method of Calendar is very verbose
                // therefore we do a toString for these objects based
                // on a date
                if ( value instanceof Calendar ) {
                    sb.append(value.getClass().getName());
                    sb.append('(');
                    sb.append(((Calendar)value).getTime());
                    sb.append(')');
                } else {
                    sb.append(value);
                }
            }
        }

    }
    /**
     * Improved toString method for a job.
     * This method prints out the job topic and all of the properties.
     */
    public static String toString(final String jobTopic,
            final String name,
            final Map<String, Object> properties) {
        final StringBuilder sb = new StringBuilder("Sling Job ");
        sb.append("[topic=");
        sb.append(jobTopic);
        if ( name != null ) {
            sb.append(", name=");
            sb.append(name);
        }
        appendProperties(sb, properties);

        sb.append("]");
        return sb.toString();
    }

    /**
     * Improved toString method for a job.
     * This method prints out the job topic and all of the properties.
     */
    public static String toString(final Job job) {
        final StringBuilder sb = new StringBuilder("Sling Job ");
        sb.append("[topic=");
        sb.append(job.getTopic());
        sb.append(", id=");
        sb.append(job.getId());
        if ( job.getName() != null ) {
            sb.append(", name=");
            sb.append(job.getName());
        }
        appendProperties(sb, ((JobImpl)job).getProperties());
        sb.append("]");
        return sb.toString();
    }

    /**
     * Read a job
     */
    public static JobImpl readJob(final Logger logger, final Resource resource) {
        JobImpl job = null;
        if ( resource != null ) {
            try {
                final ValueMap vm = ResourceHelper.getValueMap(resource);

                // check job topic and job id
                final String errorMessage = Utility.checkJobTopic(vm.get(ResourceHelper.PROPERTY_JOB_TOPIC));
                final String jobId = vm.get(ResourceHelper.PROPERTY_JOB_ID, String.class);
                if ( errorMessage == null && jobId != null ) {
                    final String topic = vm.get(ResourceHelper.PROPERTY_JOB_TOPIC, String.class);
                    final Map<String, Object> jobProperties = ResourceHelper.cloneValueMap(vm);

                    jobProperties.put(JobImpl.PROPERTY_RESOURCE_PATH, resource.getPath());
                    // convert to integers (JCR supports only long...)
                    jobProperties.put(Job.PROPERTY_JOB_RETRIES, vm.get(Job.PROPERTY_JOB_RETRIES, Integer.class));
                    jobProperties.put(Job.PROPERTY_JOB_RETRY_COUNT, vm.get(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class));
                    if ( vm.get(Job.PROPERTY_JOB_PROGRESS_STEPS) != null ) {
                        jobProperties.put(Job.PROPERTY_JOB_PROGRESS_STEPS, vm.get(Job.PROPERTY_JOB_PROGRESS_STEPS, Integer.class));
                    }
                    if ( vm.get(Job.PROPERTY_JOB_PROGRESS_STEP) != null ) {
                        jobProperties.put(Job.PROPERTY_JOB_PROGRESS_STEP, vm.get(Job.PROPERTY_JOB_PROGRESS_STEP, Integer.class));
                    }
                    @SuppressWarnings("unchecked")
                    final List<Exception> readErrorList = (List<Exception>) jobProperties.get(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
                    if ( readErrorList != null ) {
                        for(final Exception e : readErrorList) {
                            logger.warn("Unable to read job from " + resource.getPath(), e);
                        }
                    }
                    job = new JobImpl(topic,
                            (String)jobProperties.get(ResourceHelper.PROPERTY_JOB_NAME),
                            jobId,
                            jobProperties);
                } else {
                    if ( errorMessage != null ) {
                        logger.warn("{} : {}", errorMessage, resource.getPath());
                    } else if ( jobId == null ) {
                        logger.warn("Discarding job - no job id found : {}", resource.getPath());
                    }
                    // remove the job as the topic is invalid anyway
                    try {
                        resource.getResourceResolver().delete(resource);
                        resource.getResourceResolver().commit();
                    } catch ( final PersistenceException ignore) {
                        logger.debug("Unable to remove job resource.", ignore);
                    }
                }
            } catch (final InstantiationException ie) {
                // something happened with the resource in the meantime
                logger.debug("Unable to instantiate resource.", ie);
            }

        }
        return job;
    }

    private static final Comparator<Resource> RESOURCE_COMPARATOR = new Comparator<Resource>() {

        @Override
        public int compare(final Resource o1, final Resource o2) {
            final int value1 = Integer.valueOf(o1.getName());
            final int value2 = Integer.valueOf(o2.getName());
            if ( value1 < value2 ) {
                return -1;
            } else if ( value1 > value2 ) {
                return 1;
            }
            return 0;
        }
    };

    /**
     * Helper method to read all children of a resource and sort them by name
     * @param type The type of resources (for debugging)
     * @param rsrc The parent resource
     * @return Sorted list of children.
     */
    public static List<Resource> getSortedChildren(final Logger logger, final String type, final Resource rsrc) {
        final List<Resource> children = new ArrayList<Resource>();
        final Iterator<Resource> monthIter = rsrc.listChildren();
        while ( monthIter.hasNext() ) {
            final Resource monthResource = monthIter.next();
            children.add(monthResource);
            logger.debug("Found {} : {}",  type, monthResource.getName());
        }
        Collections.sort(children, RESOURCE_COMPARATOR);
        return children;
    }

}
