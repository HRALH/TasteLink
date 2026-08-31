import { useParams } from 'react-router-dom'
import FollowList from '../../components/FollowList'

export default function FollowersPage() {
  const { id } = useParams<{ id: string }>()
  return <FollowList userId={Number(id)} mode="followers" />
}
