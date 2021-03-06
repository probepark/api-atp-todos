package com.apt.todos.config

import java.util.Optional
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Order
import org.springframework.data.web.SortDefault
import org.springframework.data.web.SortDefault.SortDefaults
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.util.StringUtils
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange

class ReactiveSortHandlerMethodArgumentResolver : SyncHandlerMethodArgumentResolver {

    private var fallbackSort = DEFAULT_SORT
    /**
     * The request parameter to lookup sort information from. Defaults to `sort`. Must not be `null` or empty.
     */
    private var sortParameter = DEFAULT_PARAMETER
        set(value) {
            Assert.hasText(value, "SortParameter must not be null nor empty!")
            field = value
        }
    /**
     * The delimiter used to separate property references and the direction to be sorted by. Defaults to
     * `,`, which means sort values look like this: `firstname,lastname,asc`. Must not be `null` or empty.
     */
    var propertyDelimiter = DEFAULT_PROPERTY_DELIMITER
        set(value) {
            Assert.hasText(value, "Property delimiter must not be empty!")
            field = value
        }
    private var qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
     */
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        Sort::class.java == parameter.parameterType

    /*
     *(non-Javadoc)
     * @see org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver#resolveArgumentValue(org.springframework.core.MethodParameter, org.springframework.web.reactive.BindingContext, org.springframework.web.server.ServerWebExchange)
     */
    override fun resolveArgumentValue(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Sort {

        val directionParameter = exchange.request.queryParams[getSortParameter(parameter)]
            ?: return getDefaultFromAnnotationOrFallback(parameter)

        // Single empty parameter, e.g "sort="
        return if (directionParameter.size == 1 && !StringUtils.hasText(directionParameter[0])) {
            getDefaultFromAnnotationOrFallback(parameter)
        } else // No parameter
            parseParameterIntoSort(directionParameter, propertyDelimiter)
    }

    /**
     * Configures the delimiter used to separate the qualifier from the sort parameter. Defaults to `_`, so a
     * qualified sort property would look like `qualifier_sort`.
     *
     * @param qualifierDelimiter the qualifier delimiter to be used or `null` to reset to the default.
     */
    fun setQualifierDelimiter(qualifierDelimiter: String?) {
        this.qualifierDelimiter = qualifierDelimiter ?: DEFAULT_QUALIFIER_DELIMITER
    }

    /**
     * Configures the [Sort] to be used as fallback in case no [SortDefault] or [SortDefaults] (the
     * latter only supported in legacy mode) can be found at the method parameter to be resolved.
     *
     * If you set this to `null`, be aware that you controller methods will get `null` handed into them
     * in case no [Sort] data can be found in the request.
     *
     * @param fallbackSort the [Sort] to be used as general fallback.
     */
    fun setFallbackSort(fallbackSort: Sort) {
        this.fallbackSort = fallbackSort
    }

    /**
     * Reads the default [Sort] to be used from the given [MethodParameter]. Rejects the parameter if both an
     * [SortDefaults] and [SortDefault] annotation is found as we cannot build a reliable [Sort]
     * instance then (property ordering).
     *
     * @param parameter will never be `null`.
     * @return the default [Sort] instance derived from the parameter annotations or the configured fallback-sort
     * [setFallbackSort].
     */
    private fun getDefaultFromAnnotationOrFallback(parameter: MethodParameter): Sort {

        val annotatedDefaults = parameter.getParameterAnnotation(SortDefaults::class.java)
        val annotatedDefault = parameter.getParameterAnnotation(SortDefault::class.java)

        if (annotatedDefault != null && annotatedDefaults != null) {
            throw IllegalArgumentException(
                "Cannot use both @$SORT_DEFAULTS_NAME and @$SORT_DEFAULT_NAME on parameter $parameter! " +
                    "Move $SORT_DEFAULT_NAME into $SORT_DEFAULTS_NAME to define sorting order!"
            )
        }

        if (annotatedDefault != null) {
            return appendOrCreateSortTo(annotatedDefault, Sort.unsorted())
        }

        if (annotatedDefaults != null) {

            var sort = Sort.unsorted()

            for (currentAnnotatedDefault in annotatedDefaults.value) {
                sort = appendOrCreateSortTo(currentAnnotatedDefault, sort)
            }

            return sort
        }

        return fallbackSort
    }

    /**
     * Creates a new [Sort] instance from the given [SortDefault] or appends it to the given [Sort]
     * instance if it's not `null`.
     *
     * @param sortDefault
     * @param sortOrNull
     * @return
     */
    private fun appendOrCreateSortTo(sortDefault: SortDefault, sortOrNull: Sort): Sort {
        val fields = getSpecificPropertyOrDefaultFromValue<Array<String>>(sortDefault, "sort")

        return if (fields.isEmpty()) Sort.unsorted() else sortOrNull.and(Sort.by(sortDefault.direction, *fields))
    }

    /**
     * Returns the sort parameter to be looked up from the request. Potentially applies qualifiers to it.
     *
     * @param parameter can be `null`.
     * @return the sort parameter to be looked up from the request.
     */
    protected fun getSortParameter(parameter: MethodParameter?): String {
        val builder = StringBuilder()

        val qualifier = parameter?.getParameterAnnotation(Qualifier::class.java)

        if (qualifier != null) {
            builder.append(qualifier.value).append(qualifierDelimiter)
        }

        return builder.append(sortParameter).toString()
    }

    /**
     * Parses the given sort expressions into a [Sort] instance. The implementation expects the sources to be a
     * concatenation of Strings using the given delimiter. If the last element can be parsed into a [Direction] it's
     * considered a [Direction] and a simple property otherwise.
     *
     * @param source will never be `null`.
     * @param delimiter the delimiter to be used to split up the source elements, will never be `null`.
     * @return the parsed [Sort].
     */
    private fun parseParameterIntoSort(source: List<String?>, delimiter: String): Sort {

        val allOrders = mutableListOf<Order>()

        for (part in source) {

            if (part == null) {
                continue
            }

            val elements = part.split(delimiter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            val direction = if (elements.isEmpty()) {
                Optional.empty()
            } else {
                Direction.fromOptionalString(elements[elements.size - 1])
            }

            val lastIndex = direction.map { elements.size - 1 }.orElseGet { elements.size }

            for (i in 0 until lastIndex) {
                toOrder(elements[i], direction).ifPresent { allOrders.add(it) }
            }
        }

        return if (allOrders.isEmpty()) Sort.unsorted() else Sort.by(allOrders)
    }

    /**
     * Folds the given [Sort] instance into a [List] of sort expressions, accumulating [Order] instances
     * of the same direction into a single expression if they are in order.
     *
     * @param sort must not be `null`.
     * @return the list of sort expresssions.
     */
    private fun foldIntoExpressions(sort: Sort): List<String> {

        val expressions = mutableListOf<String>()
        var builder: ExpressionBuilder? = null

        for (order in sort) {

            val direction = order.direction

            if (builder == null) {
                builder = ExpressionBuilder(direction)
            } else if (!builder.hasSameDirectionAs(order)) {
                builder.dumpExpressionIfPresentInto(expressions)
                builder = ExpressionBuilder(direction)
            }

            builder.add(order.property)
        }

        return builder?.dumpExpressionIfPresentInto(expressions) ?: emptyList()
    }

    /**
     * Folds the given [Sort] instance into two expressions. The first being the property list, the second being the
     * direction.
     *
     * @throws IllegalArgumentException if a [Sort] with multiple [Direction]s has been handed in.
     * @param sort must not be `null`.
     * @return the list of epxressions.
     */
    private fun legacyFoldExpressions(sort: Sort): List<String> {

        val expressions = mutableListOf<String>()
        var builder: ExpressionBuilder? = null

        for (order in sort) {

            val direction = order.direction

            if (builder == null) {
                builder = ExpressionBuilder(direction)
            } else if (!builder.hasSameDirectionAs(order)) {
                throw IllegalArgumentException(
                    "${javaClass.simpleName} in legacy configuration only supports a single direction to sort by!"
                )
            }

            builder.add(order.property)
        }

        return builder?.dumpExpressionIfPresentInto(expressions) ?: emptyList()
    }

    /**
     * Helper to easily build request parameter expressions for [Sort] instances.
     *
     * @author Oliver Gierke
     */
    internal inner class ExpressionBuilder
    /**
     * Sets up a new [ExpressionBuilder] for properties to be sorted in the given [Direction].
     */
        (private val direction: Direction) {

        private val elements = mutableListOf<String>()

        /**
         * Returns whether the given [Order] has the same direction as the current [ExpressionBuilder].
         *
         * @param order must not be null.
         * @return
         */
        fun hasSameDirectionAs(order: Order) = this.direction == order.direction

        /**
         * Adds the given property to the expression to be built.
         *
         * @param property the property to add.
         */
        fun add(property: String) {
            this.elements.add(property)
        }

        /**
         * Dumps the expression currently in build into the given [List] of [String]s. Will only dump it in case
         * there are properties piled up currently.
         *
         * @param expressions
         * @return
         */
        fun dumpExpressionIfPresentInto(expressions: MutableList<String>): List<String> {

            if (elements.isEmpty()) {
                return expressions
            }

            elements.add(direction.name.toLowerCase())
            expressions.add(StringUtils.collectionToDelimitedString(elements, propertyDelimiter))

            return expressions
        }
    }

    companion object {

        private const val DEFAULT_PARAMETER = "sort"
        private const val DEFAULT_PROPERTY_DELIMITER = ","
        private const val DEFAULT_QUALIFIER_DELIMITER = "_"
        private val DEFAULT_SORT = Sort.unsorted()

        private val SORT_DEFAULTS_NAME = SortDefaults::class.java.simpleName
        private val SORT_DEFAULT_NAME = SortDefault::class.java.simpleName

        private fun toOrder(property: String, direction: Optional<Direction>) =
            if (!StringUtils.hasText(property)) {
                Optional.empty()
            } else {
                Optional.of(direction.map { Order(it, property) }.orElseGet { Order.by(property) })
            }

        /**
         * Returns the value of the given specific property of the given annotation. If the value of that property is the
         * properties default, we fall back to the value of the `value` attribute.
         *
         * @param annotation must not be `null`.
         * @param property must not be `null` or empty.
         * @return the value of the given specific property of the given annotation.
         */
        fun <T> getSpecificPropertyOrDefaultFromValue(annotation: Annotation, property: String): T {

            val propertyDefaultValue = AnnotationUtils.getDefaultValue(annotation, property)
            val propertyValue = AnnotationUtils.getValue(annotation, property)

            val result = (
                if (ObjectUtils.nullSafeEquals(propertyDefaultValue, propertyValue))
                    AnnotationUtils.getValue(annotation)
                else
                    propertyValue
                )
                ?: throw IllegalStateException("Expected to be able to look up an annotation property value but failed!")

            return result as T
        }
    }
}
