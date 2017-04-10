package ru.yuksanbo.common.jmx

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import org.slf4j.LoggerFactory

import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import java.lang.management.ManagementFactory
import java.util.stream.Collectors

object MXBeans {

    private val LOG = LoggerFactory.getLogger(MXBeans::class.java)

    /**
     * Register MXBean object in JMX.
     * If keyProperties doesn't contain property 'type', it will be set as mxBean class name.
     */
    fun <T : Any> registerMXBean(mxBean: T, domain: String, keyProperties: Map<String, String>): T {
        Preconditions.checkNotNull(mxBean, "mxBean is null")
        Preconditions.checkNotNull(domain, "domain is null")
        Preconditions.checkNotNull(keyProperties, "keyProperties is null")

        val typeProperty = if (keyProperties.containsKey("type")) {
            "type=" + keyProperties["type"]
        }
        else {
            "type=" + mxBean.javaClass.simpleName
        }

        val otherProperties = if (keyProperties.isEmpty() || keyProperties.size == 1 && keyProperties.containsKey("type")) {
            ""
        }
        else {
            keyProperties
                    .entries
                    .stream()
                    .filter { e -> e.key != "type" }
                    .map<String> { e -> e.key + "=" + e.value }
                    .collect(Collectors.joining(",", ",", ""))
        }

        val mxBeanNameText = String.format("%s:%s%s", domain, typeProperty, otherProperties)

        val mxBeanObjectName: ObjectName
        try {
            mxBeanObjectName = ObjectName(mxBeanNameText)
        }
        catch (e: MalformedObjectNameException) {
            throw RuntimeException(String.format("Malformed MXBean name '%s'", mxBeanNameText), e)
        }

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, mxBeanObjectName)
        }
        catch (e: Exception) {
            throw RuntimeException(
                    String.format("Failed to register %s as MXBean with name '%s'", mxBean, mxBeanObjectName),
                    e
            )
        }

        LOG.debug("MXBean {} registered with name '{}'", mxBean, mxBeanObjectName)

        return mxBean
    }

    fun <T : Any> registerMXBean(mxBean: T, mxBeanIdentity: MXBeanIdentity): T {
        return registerMXBean(
                mxBean,
                mxBeanIdentity.domain,
                mapOf(
                        "type" to mxBeanIdentity.type,
                        "name" to mxBeanIdentity.name
                )
        )
    }

    fun <T : Any> registerMXBean(mxBean: T, keyProperties: Map<String, String>): T {
        return registerMXBean(mxBean, mxBean.javaClass.`package`.name, keyProperties)
    }

    fun <T : Any> registerMXBean(mxBean: T, domain: String, mxBeanName: String): T {
        return registerMXBean(mxBean, domain, ImmutableMap.of("name", mxBeanName))
    }

    fun <T : Any> registerMXBean(mxBean: T, mxBeanName: String): T {
        return registerMXBean(mxBean, ImmutableMap.of("name", mxBeanName))
    }

    fun <T : Any> registerMXBean(mxBean: T): T {
        return registerMXBean(mxBean, ImmutableMap.of())
    }

    /**
     * Removes special characters, used in mbean names.
     */
    fun adaptBeanName(name: String): String {
        return name.replace("[:,*?\"=]+".toRegex(), "")
    }

}
