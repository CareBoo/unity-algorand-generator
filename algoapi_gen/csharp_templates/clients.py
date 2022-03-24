file_template = """\
{header}

{imports}

namespace {namespace}
{
    public interface I{client_name} : IAlgoApiClient
    {
        {interface_definitions}
    }

    public partial struct {client_name} : I{client_name}
    {
        {client_methods}
    }
}

"""

interface_definition_template = """\
{method_xml_doc}
AlgoApiRequest.Sent{response_type_generic} {method_id}({method_params});
"""

client_method_template = """\
public AlgoApiRequest.Sent{response_type_generic} {method_id}({method_params})
{
    {query_builder}
    return this
        {http_method_action}({endpoint}){set_request_body_action}
        .Send();
}
"""
