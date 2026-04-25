package model

// AbstractModel mirrors Java AbstractModel: every persisted entity has _id (string)
// and a `class` discriminator equal to the simple type name.
type AbstractModel struct {
	ID    string `bson:"_id" json:"_id"`
	Class string `bson:"class" json:"class"`
}
