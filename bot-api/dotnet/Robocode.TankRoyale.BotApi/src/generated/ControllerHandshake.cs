//----------------------
// <auto-generated>
//     Generated using the NJsonSchema v10.7.0.0 (Newtonsoft.Json v9.0.0.0) (http://NJsonSchema.org)
// </auto-generated>
//----------------------


namespace Robocode.TankRoyale.Schema
{
    #pragma warning disable // Disable all warnings

    /// <summary>
    /// Controller handshake
    /// </summary>
    [System.CodeDom.Compiler.GeneratedCode("NJsonSchema", "10.7.0.0 (Newtonsoft.Json v9.0.0.0)")]
    public class ControllerHandshake : Message
    {
        /// <summary>
        /// Name of controller, e.g. Fancy Robocode Controller
        /// </summary>
        [Newtonsoft.Json.JsonProperty("name", Required = Newtonsoft.Json.Required.Always)]
        [System.ComponentModel.DataAnnotations.Required(AllowEmptyStrings = true)]
        public string Name { get; set; }

        /// <summary>
        /// Controller version, e.g. 1.0
        /// </summary>
        [Newtonsoft.Json.JsonProperty("version", Required = Newtonsoft.Json.Required.Always)]
        [System.ComponentModel.DataAnnotations.Required(AllowEmptyStrings = true)]
        public string Version { get; set; }

        /// <summary>
        /// Author name, e.g. John Doe (john_doe@somewhere.net)
        /// </summary>
        [Newtonsoft.Json.JsonProperty("author", Required = Newtonsoft.Json.Required.DisallowNull, NullValueHandling = Newtonsoft.Json.NullValueHandling.Ignore)]
        public string Author { get; set; }

        /// <summary>
        /// Secret used for access control with the server
        /// </summary>
        [Newtonsoft.Json.JsonProperty("secret", Required = Newtonsoft.Json.Required.DisallowNull, NullValueHandling = Newtonsoft.Json.NullValueHandling.Ignore)]
        public string Secret { get; set; }


    }
}