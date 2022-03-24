file_template = """\
{header}

{imports}

namespace {namespace}
{
    {models}
}

"""

model_template = """\
{doc}
{attributes}
public partial struct {name}
    : IEquatable<{name}>
{
    {fields}

    {properties}

    public bool Equals({name} other)
    {
        return {equals_defn}
    }
}
"""
