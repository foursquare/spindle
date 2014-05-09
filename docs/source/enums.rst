Enums
=====

Enumerations in Thrift are defined with the ``enum`` keyword. Enumeration values have names and integer identifiers. When
compiled to Scala code, enumerations are represented by a class (of the same name as the enumeration), a companion
object, and objects for each of the enumeration values (defined inside the companion object).

Enum value methods
------------------

The following methods are available on each value of an enumeration:

* ``id`` - integer id of the enum value
* ``name`` - string name of the enum value, as represented in Thrift source (not intended to be stable)
* ``stringValue`` - string value of the enum value, as represented by ``string_value`` annotation in Thrift (intended to be stable); if there is no annotation, the name is used
* ``compare`` - compare two enum values by their ids
* ``meta`` - access the companion object for the enum

Companion object methods
------------------------

The following methods are availabel on an enumeration's companion object:

* ``findById`` - given an integer id, return an ``Option`` of the enum value with that id, or ``None``
* ``findByIdOrNull`` - given an integer id, return the enum value with that id, or ``null``
* ``findByName`` - given a string name, return an Option of the enum value with that name, or ``None``
* ``findByNameOrNull`` - given a string name, return the enum value with that name, or ``null``
* ``findByStringValue`` - given an string value, return an Option of the enum value with that string value, or ``None``
* ``findByStringValueOrNull`` - given an string value, return the enum value with that string value, or ``null``
* ``unapply`` - alias for findByName (TODO: should be findByStringValue)

Matching and unknown values
---------------------------

Spindle will generate an additional value in every enum called ``UnknownWireValue``. This value is meant to handle
values read off the wire that do not correspond to any known enum value. This can happen when a value is added to an
enum, and old code tries to deserialize a value written by new code. So in order for your enum matches to be exhaustive,
you must also match against ``UnknownWireValue(id)``.

Examples
--------

Example thrift::

    enum ClientType {
      Android = 1
      Blackberry = 2
      IPhone = 3
      Web = 4
    }

Example Scala::

    val client: ClientType = ClientType.web

    client match {
      case ClientType.Android => ...
      case ClientType.Blackberry => ...
      case ClientType.IPhone => ...
      case ClientType.Web => ...
      case ClientType.UnknownWireValue(id) => ???
    }


