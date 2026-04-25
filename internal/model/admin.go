package model

// AdminID is the singleton document _id used by Java AdminRepository.
const AdminID = "admin"

type Admin struct {
	AbstractModel `bson:",inline"`
	Groups        []int64 `bson:"groups" json:"groups"`
}

func NewAdmin() *Admin {
	return &Admin{
		AbstractModel: AbstractModel{ID: AdminID, Class: "Admin"},
		Groups:        []int64{},
	}
}

func (a *Admin) AddGroup(groupID int64) bool {
	if a.HasGroup(groupID) {
		return false
	}
	a.Groups = append(a.Groups, groupID)
	return true
}

func (a *Admin) RemoveGroup(groupID int64) bool {
	for i, g := range a.Groups {
		if g == groupID {
			a.Groups = append(a.Groups[:i], a.Groups[i+1:]...)
			return true
		}
	}
	return false
}

func (a *Admin) HasGroup(groupID int64) bool {
	for _, g := range a.Groups {
		if g == groupID {
			return true
		}
	}
	return false
}
